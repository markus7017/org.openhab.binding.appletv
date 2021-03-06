"""CLI application for discovering and controlling Apple TVs."""

import sys
import inspect
import logging
import binascii
import asyncio
import traceback

import argparse
from argparse import ArgumentTypeError
from zeroconf import Zeroconf

import pyatv
import pyatv.pairing
from pyatv import (const, dmap, exceptions, interface, tag_definitions)
from pyatv.interface import retrieve_commands

import jpy

javaHandler = None

class GlobalCommands:
    """Commands not bound to a specific device."""
    def __init__(self, args, loop):
        """Initialize a new instance of GlobalCommands."""
        self.args = args
        self.loop = loop

    @asyncio.coroutine
    def help(self):
        """Print help text for a command."""
        if len(self.args.command) != 2:
            """print('Which command do you want help with?', file=sys.stderr)"""
            return 1

        iface = [interface.RemoteControl,
                 interface.Metadata,
                 interface.Playing,
                 interface.AirPlay,
                 self.__class__,
                 DeviceCommands]
        for cmd in iface:
            for key, value in cmd.__dict__.items():
                if key.startswith('_') or key != self.args.command[1]:
                    continue

                if inspect.isfunction(value):
                    signature = inspect.signature(value)
                else:
                    signature = ' (property)'

                print('COMMAND:\n>> {0}{1}\n\nHELP:\n{2}'.format(
                    key, signature, inspect.getdoc(value)))
        return 0

    @asyncio.coroutine
    def scan(self):
        """Scan for Apple TVs on the network."""
        atvs = yield from pyatv.scan_for_apple_tvs(
            self.loop, timeout=self.args.scan_timeout, only_home_sharing=False)
        _print_found_apple_tvs(atvs)
        return 0

    @asyncio.coroutine
    def pair(self):
        """Pair pyatv as a remote control with an Apple TV."""
        global javaHandler
        handler = pyatv.pair_with_apple_tv(
            self.loop, self.args.pin_code, self.args.remote_name,
            pairing_guid=self.args.pairing_guid)
        javaHandler.info('Using pairing guid: 0x' + handler.pairing_guid)
        if self.args.pin_code is None:
            #print('Use any pin to pair with "{}" (press ENTER to stop)'.format(
            #    self.args.remote_name))
            javaHandler.info("ERROR: PIN missing!")
            raise AuthenticationError('PIN missing!')
        else:
            message = 'Use pin {} to pair with "{}"'.format(
                self.args.pin_code, self.args.remote_name)
            javaHandler.info(message)
        javaHandler.info('Note: If remote does not show up, try rebooting your Apple TV')

        yield from handler.start(Zeroconf())
        yield from self.loop.run_in_executor(None, sys.stdin.readline)
        yield from handler.stop()

        # Give some feedback to the user
        if handler.has_paired:
            javaHandler.info('Pairing seems to have succeeded, yey!')
            javaHandler.info('Login id from paring: 0x{}'.format(
                handler.pairing_guid))
            javaHandler.statusEvent("login_id", handler.pairing_guid())
        else:
            javaHandler.info('ERROR: Timeout on pairing!')
            raise AuthenticationError('Timeout on pairing!')
            return 1

        return 0


class DeviceCommands:
	"""Additional commands available for a device.

	These commands are not part of the API but are provided by atvremote.
	"""

	def __init__(self, atv, loop):
		"""Initialize a new instance of DeviceCommands."""
		self.atv = atv
		self.loop = loop

	@asyncio.coroutine
	def artwork_save(self):
		"""Download artwork and save it to artwork.png."""
		global javaHandler
		artwork = yield from self.atv.metadata.artwork()
		if artwork is not None:
			fname = '/tmp/ohpyatv-artwork.png'
			javaHandler.info('Saving artwork to {0}'.format(fname))
			with open(fname, 'wb') as file:
				file.write(artwork)
		else:
			javaHandler.info('No artwork is currently available.')
			return 1
		return 0

	@asyncio.coroutine
	def push_updates(self):
		"""Listen for push updates."""
		#print('Press ENTER to stop')
		#self.atv.push_updater.start()
		#yield from self.loop.run_in_executor(None, sys.stdin.readline)
		#self.atv.push_updater.stop()
		#return 0
		return 1

	@asyncio.coroutine
	def auth(self):
		"""Perform AirPlay device authentication."""
		global javaHandler
		credentials = yield from self.atv.airplay.generate_credentials()
		yield from self.atv.airplay.load_credentials(credentials)

		try:
			yield from self.atv.airplay.start_authentication()
			pin = self.args.pin
			if pin is None:
				#pin = input('Enter PIN on screen: ')
				javaHandler.info('WARNING: Unable to authenticate, PIN missing!')
				raise AuthenticationError('PIN missing!')
			yield from self.atv.airplay.finish_authentication(pin)
			javaHandler.info('Credentials from authentication {0}'.format(credentials))
			javaHandler.statusEvent("credentials", credentials)
			return 0

		except exceptions.DeviceAuthenticationError:
			logging.exception('Failed to authenticate - no/invalid PIN?')
			return 1


class PushListener:
    """Internal listener for push updates."""

    @staticmethod
    def playstatus_update(_, playstatus):
        """Print what is currently playing when it changes."""
        print('Play Status:')
        print(str(playstatus))
        print(20*'-')

    @staticmethod
    def playstatus_error(updater, exception):
        """Inform about an error and restart push updates."""
        print("An error occurred (restarting): {0}".format(exception))
        updater.start(initial_delay=1)


def _in_range(lower, upper, allow_none=False):
    def _checker(value):
        if allow_none and str(value).lower() == 'none':
            return None
        if int(value) >= lower and int(value) < upper:
            return int(value)
        raise ArgumentTypeError('Must be greater >= {} and < {}'.format(
            lower, upper))
    return _checker

@asyncio.coroutine
def cli_handler(loop, jargs):
	"""Application starts here."""	
	parser = argparse.ArgumentParser()

	parser.add_argument('command', nargs='+',
	                    help='commands, help, ...')
	parser.add_argument('--name', help='apple tv name',
	                    dest='name', default='Apple TV')
	parser.add_argument('--address', help='device ip address or hostname',
	                    dest='address', default=None)
	parser.add_argument('-t', '--scan-timeout', help='timeout when scanning',
	                    dest='scan_timeout', type=_in_range(1, 10),
	                    metavar='TIMEOUT', default=3)
	parser.add_argument('--version', action='version',
	                    help='version of atvremote and pyatv',
	                    version='%(prog)s {0}'.format(const.__version__))

	pairing = parser.add_argument_group('pairing')
	pairing.add_argument('--remote-name', help='remote pairing name',
	                    dest='remote_name', default='pyatv')
	pairing.add_argument('-p', '--pin', help='pairing pin code',
	                    dest='pin_code', metavar='PIN', default=1234,
	                 type=_in_range(0, 9999, allow_none=True))
	pairing.add_argument('--pairing-guid',
	                    help='pairing guid (16 chars hex)',
	                    dest='pairing_guid', default=None)

	ident = parser.add_mutually_exclusive_group()
#	ident.add_argument('-a', '--autodiscover',
#	                   help='automatically find a device',
#	                   action='store_true', dest='autodiscover', default=False)
	ident.add_argument('--login_id', help='home sharing id or pairing guid',
	                   dest='login_id', default=None)

	airplay = parser.add_argument_group('airplay')
	airplay.add_argument('--airplay_credentials',
	                     help='credentials for airplay',
	                     dest='airplay_credentials', default=None)

	debug = parser.add_argument_group('debugging')
	debug.add_argument('-v', '--verbose', help='increase output verbosity',
	                   action='store_true', dest='verbose')
	debug.add_argument('--debug', help='print debug information',
	                   action='store_true', dest='debug')

	args = parser.parse_args(jargs)
	loglevel = logging.WARNING
	if args.verbose:
	    loglevel = logging.INFO
	if args.debug:
	    loglevel = logging.DEBUG
	logging.basicConfig(level=loglevel,
	                    format='%(levelname)s: %(message)s')
	logging.getLogger('requests').setLevel(logging.WARNING)

	# Sanity checks that not can be done natively by argparse
	if (args.login_id and not args.address) or \
	    (not args.login_id and args.address):
	    parser.error('both --login_id and --address must be given')

	cmds = retrieve_commands(GlobalCommands)

	if args.command[0] in cmds:
		glob_cmds = GlobalCommands(args, loop)
		return (yield from _exec_command(
           	glob_cmds, args.command[0], print_result=False))
#	if args.autodiscover:
#		return (yield from _handle_autodiscover(args, loop))
	if args.login_id:
		return (yield from _handle_commands(args, loop))

#	logging.error('To autodiscover an Apple TV, add -a')
	return 1

def _print_found_apple_tvs(atvs, outstream=sys.stdout):
	global javaHandler
	javaHandler.debug('Discovery completed')
	jsonDevices = ""
	i = 0
	try:
		for apple_tv in atvs:
			if apple_tv.login_id is None:
				msg = ' - {0} at {1} (home sharing disabled)'.format(apple_tv.name, apple_tv.address)
			else:
				msg = ' - {0} at {1} (login id: {2})'.format(apple_tv.name, apple_tv.address, apple_tv.login_id)
			javaHandler.info(str(msg))
			i = i+1
			if i != 1:
				jsonDevices = jsonDevices+", "
			inner = '"name":"{0}", "ipAddress":"{1}", "loginId":"{2}"'.format(apple_tv.name, apple_tv.address, apple_tv.login_id)
			jsonDevices = jsonDevices + "{ "+inner+" }"

		javaHandler.devicesDiscovered(str('{ "devices": [ '+ jsonDevices  +' ] }'))
		return 0

	except Exception as e:
		javaHandler.info("Exception in print_found_apple_tv: "+str(e))
		return 1


#@asyncio.coroutine
#def _handle_autodiscover(args, loop):
#	atvs = yield from pyatv.scan_for_apple_tvs(
#	    loop, timeout=args.scan_timeout, abort_on_found=True)
#
#	if not atvs:
#		logging.error('Could not find any Apple TV on current network')
#		return 1
#
#	if len(atvs) > 1:
#		logging.error('Found more than one Apple TV; '
#		              'specify one using --address and --login_id')
#		_print_found_apple_tvs(atvs, outstream=sys.stderr)
#		return 1
#
#	# Simple hack to re-use existing command handling and respect options
#	apple_tv = atvs[0]
#	args.address = apple_tv.address
#	args.login_id = apple_tv.login_id
#	args.name = apple_tv.name
#	logging.info('Auto-discovered device {0} at {1}'.format(args.name, args.address))
#
#	return (yield from _handle_commands(args, loop))

def _extract_command_with_args(cmd):
	"""Parse input command with arguments.
	Parses the input command in such a way that the user may
	provide additional argument to the command. The format used is this:
	command=arg1,arg2,arg3,...
	all the additional arguments are passed as arguments to the target
	method.
	"""
	equal_sign = cmd.find('=')
	if equal_sign == -1:
		return cmd, []

	command = cmd[0:equal_sign]
	args = cmd[equal_sign+1:].split(',')
	return command, args


@asyncio.coroutine
def _handle_commands(args, loop):
	#print('_handle_commands: name={0}, address={1}, login_id={2}'.format(args.name, args.address, args.login_id))
	details = pyatv.AppleTVDevice(args.name, args.address, args.login_id)
	atv = pyatv.connect_to_apple_tv(details, loop)
	atv.push_updater.listener = PushListener()

	try:
		if args.airplay_credentials is not None:
			yield from atv.airplay.load_credentials(args.airplay_credentials)

		for cmd in args.command:
			print('process cmd "{0}"'.format(str(cmd)))
			if cmd is None:
				break
			ret = yield from _handle_device_command(args, cmd, atv, loop)
			if ret != 0:
				return ret
	finally:
		yield from atv.logout()

	return 0

# pylint: disable=too-many-return-statements
@asyncio.coroutine
def _handle_device_command(args, cmdline, atv, loop):
	# TODO: Add these to array and use a loop
	device = retrieve_commands(DeviceCommands)
	ctrl = retrieve_commands(interface.RemoteControl)
	metadata = retrieve_commands(interface.Metadata)
	playing = retrieve_commands(interface.Playing)
	airplay = retrieve_commands(interface.AirPlay)

	# Parse input command and argument from user
	cmd, cmd_args = _extract_command_with_args(cmdline)
	if cmd in device:
		return (yield from _exec_command(
			DeviceCommands(atv, loop), cmd, False, *cmd_args))

	if cmd in ctrl:
		return (yield from _exec_command(
			atv.remote_control, cmd, True, *cmd_args))

	if cmd in metadata:
		return (yield from _exec_command(
			atv.metadata, cmd, True, *cmd_args))

	if cmd in playing:
		playing_resp = yield from atv.metadata.playing()
		return (yield from _exec_command(
			playing_resp, cmd, True, *cmd_args))

	if cmd in airplay:
		return (yield from _exec_command(
				atv.airplay, cmd, True, *cmd_args))

	logging.error('Unknown command: %s', args.command[0])
	return 1

@asyncio.coroutine
def _exec_command(obj, command, print_result, *args):
	global javaHandler
	try:
		# If the command to execute is a @property, the value returned by that
		# property will be stored in tmp. Otherwise it's a coroutine and we
		# have to yield for the result and wait until it is available.
		tmp = getattr(obj, command)
		if inspect.ismethod(tmp):
			value = yield from tmp(*args)
		else:
			value = tmp

		# Some commands might produce output themselves (especially non-API
		# commands), so don't print the return code they might give
		if print_result:
			_pretty_print(value)
			return 0
		return value
	except NotImplementedError:
		javaHandler.info("Command '%s' is not supported by device", command)
	except exceptions.AuthenticationError as ex:
		javaHandler.info('Authentication error: %s', str(ex))
	except Exception as e:
		javaHandler.info("Exception in _exec_command(): "+str(e))
		traceback.print_exc(file=sys.stderr)
		return 1
	return 1

def _pretty_print(data):
	global javaHandler
	try:
		if data is None:
			return
		if isinstance(data, bytes):
			print(binascii.hexlify(data))
		elif isinstance(data, list):
			print(dmap.pprint(data, tag_definitions.lookup_tag))
		else:
			print(data)
	except Exception as e:
		javaHandler.debug("Exception in _pretty_print(): "+str(e))
		traceback.print_exc(file=sys.stderr)

class PyATV:
	def init(self, handler):
		#sys.stdout = open('/tmp/ohpyatv-console.log', 'w')
		#sys.stderr = open('/tmp/ohpyatv-error.log', 'w')
		global javaHandler
		try:
			print('Initialize Java access', flush=True)
			javaHandler = handler
			javaHandler.info('Embeeded PyATV {0}'.format(const.__version__))
		except Exception as e:
			print("Unable to access Java class: "+str(e), flush=True)
			return 1
		return 0				

	def exec(self, handler, jargs):
		"""Start the asyncio event loop and runs the application."""
		# Helper method so that the coroutine exits cleanly if an exception
		# happens (which would leave resources dangling)
		@asyncio.coroutine
		def _run_application(loop, jargs):
			global javaHandler
			try:
				return (yield from cli_handler(loop, jargs))
			except SystemExit:
				pass  # sys.exit() was used - do nothing
			except Exception as e:
				javaHandler.info("Exception in _run_application(): "+str(e))
				traceback.print_exc(file=sys.stderr)
				return 1

		try:
			global javaHandler
			javaHandler = handler
			self.args = jargs
			loop = asyncio.new_event_loop()
			asyncio.set_event_loop(loop)
			return loop.run_until_complete(_run_application(loop, jargs))
		except Exception as e:
			javaHandler.info("Exception in exec(): "+str(e))
			return 1

		return 0
