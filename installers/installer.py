#!/usr/bin/env python3

import os
import sys
import subprocess
import time
import logging
import argparse
import json
import threading
import signal
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, asdict
from datetime import datetime
import psutil
import yaml


@dataclass
class CommandResult:
    """Result of command execution"""
    command: str
    return_code: int
    stdout: str
    stderr: str
    execution_time: float
    success: bool
    error_message: Optional[str] = None


@dataclass
class InstallerConfig:
    """Installer configuration"""
    timeout: int = 300
    retry_attempts: int = 3
    retry_delay: int = 5
    parallel_execution: bool = False
    max_parallel_jobs: int = 4
    log_level: str = "INFO"
    log_file: Optional[str] = None
    dry_run: bool = False
    validate_prerequisites: bool = True
    monitor_resources: bool = True


class ResourceMonitor:
    """System resource monitor"""
    
    def __init__(self):
        self.start_time = time.time()
        self.peak_cpu = 0
        self.peak_memory = 0
        self.samples = []
    
    def sample(self):
        """Collects a resource sample"""
        cpu_percent = psutil.cpu_percent(interval=1)
        memory_percent = psutil.virtual_memory().percent
        
        self.peak_cpu = max(self.peak_cpu, cpu_percent)
        self.peak_memory = max(self.peak_memory, memory_percent)
        
        self.samples.append({
            'timestamp': time.time(),
            'cpu': cpu_percent,
            'memory': memory_percent
        })
    
    def get_summary(self) -> Dict[str, Any]:
        """Returns monitoring summary"""
        return {
            'duration': time.time() - self.start_time,
            'peak_cpu': self.peak_cpu,
            'peak_memory': self.peak_memory,
            'samples_count': len(self.samples)
        }


class ShellInstaller:
    """Specialist installer for executing shell scripts"""
    
    def __init__(self, config: InstallerConfig):
        self.config = config
        self.setup_logging()
        self.results: List[CommandResult] = []
        self.resource_monitor = ResourceMonitor()
        self.running = True
        
        # Configure signal handlers
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
    
    def setup_logging(self):
        """Configures the logging system"""
        log_format = '%(asctime)s - %(levelname)s - %(message)s'
        
        # Configure logging
        logging.basicConfig(
            level=getattr(logging, self.config.log_level),
            format=log_format,
            handlers=[
                logging.StreamHandler(sys.stdout),
                logging.FileHandler(self.config.log_file) if self.config.log_file else logging.NullHandler()
            ]
        )
        
        self.logger = logging.getLogger(__name__)
    
    def _signal_handler(self, signum, frame):
        """Handler for interruption signals"""
        self.logger.warning(f"Received signal {signum}. Interrupting execution...")
        self.running = False
    
    def validate_prerequisites(self) -> bool:
        """Validates system prerequisites"""
        self.logger.info("Validating prerequisites...")
        
        prerequisites = [
            ("Python", "python3", "--version"),
            ("Bash", "bash", "--version"),
            ("Git", "git", "--version"),
        ]
        
        for name, command, version_flag in prerequisites:
            try:
                result = subprocess.run(
                    [command, version_flag] if version_flag else [command],
                    capture_output=True,
                    text=True,
                    timeout=10
                )
                if result.returncode == 0:
                    self.logger.info(f"✅ {name}: OK")
                else:
                    self.logger.error(f"❌ {name}: Not found")
                    return False
            except (subprocess.TimeoutExpired, FileNotFoundError):
                self.logger.error(f"❌ {name}: Not found")
                return False
        
        # Check disk space
        disk_usage = psutil.disk_usage('/')
        free_gb = disk_usage.free / (1024**3)
        if free_gb < 5:
            self.logger.warning(f"⚠️  Low disk space: {free_gb:.1f}GB free")
        else:
            self.logger.info(f"✅ Disk space: {free_gb:.1f}GB free")
        
        # Check available memory
        memory = psutil.virtual_memory()
        available_gb = memory.available / (1024**3)
        if available_gb < 2:
            self.logger.warning(f"⚠️  Low available memory: {available_gb:.1f}GB")
        else:
            self.logger.info(f"✅ Available memory: {available_gb:.1f}GB")
        
        return True
    
    def execute_command(self, command: str, cwd: Optional[str] = None, 
                       timeout: Optional[int] = None) -> CommandResult:
        """Executes a shell command"""
        start_time = time.time()
        timeout = timeout or self.config.timeout
        
        self.logger.info(f"Executing: {command}")
        
        if self.config.dry_run:
            self.logger.info(f"[DRY RUN] Command would be executed: {command}")
            return CommandResult(
                command=command,
                return_code=0,
                stdout="[DRY RUN]",
                stderr="",
                execution_time=0,
                success=True
            )
        
        try:
            # Execute command
            process = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True,
                cwd=cwd,
                timeout=timeout
            )
            
            execution_time = time.time() - start_time
            
            result = CommandResult(
                command=command,
                return_code=process.returncode,
                stdout=process.stdout,
                stderr=process.stderr,
                execution_time=execution_time,
                success=process.returncode == 0
            )
            
            if result.success:
                self.logger.info(f"✅ Command executed successfully in {execution_time:.2f}s")
                if result.stdout.strip():
                    self.logger.debug(f"STDOUT: {result.stdout}")
            else:
                self.logger.error(f"❌ Command failed (exit code: {process.returncode})")
                if result.stderr.strip():
                    self.logger.error(f"STDERR: {result.stderr}")
                result.error_message = f"Exit code: {process.returncode}"
            
            return result
            
        except subprocess.TimeoutExpired:
            execution_time = time.time() - start_time
            self.logger.error(f"⏰ Command timed out after {timeout}s")
            return CommandResult(
                command=command,
                return_code=-1,
                stdout="",
                stderr=f"Timeout after {timeout}s",
                execution_time=execution_time,
                success=False,
                error_message="Timeout"
            )
        
        except Exception as e:
            execution_time = time.time() - start_time
            self.logger.error(f"💥 Error executing command: {e}")
            return CommandResult(
                command=command,
                return_code=-1,
                stdout="",
                stderr=str(e),
                execution_time=execution_time,
                success=False,
                error_message=str(e)
            )
    
    def execute_with_retry(self, command: str, cwd: Optional[str] = None) -> CommandResult:
        """Executes command with automatic retry"""
        for attempt in range(self.config.retry_attempts):
            if not self.running:
                break
                
            result = self.execute_command(command, cwd)
            
            if result.success:
                return result
            
            if attempt < self.config.retry_attempts - 1:
                self.logger.warning(f"Attempt {attempt + 1} failed. Waiting {self.config.retry_delay}s...")
                time.sleep(self.config.retry_delay)
        
        return result
    
    def execute_script(self, script_path: str, args: List[str] = None) -> CommandResult:
        """Executes a shell script"""
        if not os.path.exists(script_path):
            return CommandResult(
                command=script_path,
                return_code=-1,
                stdout="",
                stderr=f"Script not found: {script_path}",
                execution_time=0,
                success=False,
                error_message="Script not found"
            )
        
        # Make script executable
        os.chmod(script_path, 0o755)
        
        # Build command
        command = f"bash {script_path}"
        if args:
            command += f" {' '.join(args)}"
        
        return self.execute_with_retry(command)
    
    def execute_parallel(self, commands: List[str], max_jobs: int = None) -> List[CommandResult]:
        """Executes commands in parallel"""
        max_jobs = max_jobs or self.config.max_parallel_jobs
        results = []
        
        def worker(cmd: str) -> CommandResult:
            return self.execute_command(cmd)
        
        # Execute commands in batches
        for i in range(0, len(commands), max_jobs):
            batch = commands[i:i + max_jobs]
            threads = []
            
            for cmd in batch:
                thread = threading.Thread(target=lambda c=cmd: results.append(worker(c)))
                thread.start()
                threads.append(thread)
            
            # Wait for batch completion
            for thread in threads:
                thread.join()
        
        return results
    
    def monitor_resources(self):
        """Starts resource monitoring in background"""
        if not self.config.monitor_resources:
            return
        
        def monitor():
            while self.running:
                self.resource_monitor.sample()
                time.sleep(5)
        
        thread = threading.Thread(target=monitor, daemon=True)
        thread.start()
    
    def install_from_config(self, config_file: str) -> bool:
        """Installs based on YAML configuration file"""
        try:
            with open(config_file, 'r') as f:
                config_data = yaml.safe_load(f)
        except Exception as e:
            self.logger.error(f"Error reading configuration file: {e}")
            return False
        
        # Validate prerequisites
        if self.config.validate_prerequisites:
            if not self.validate_prerequisites():
                self.logger.error("Prerequisites validation failed")
                return False
        
        # Start monitoring
        self.monitor_resources()
        
        # Execute commands
        commands = config_data.get('commands', [])
        scripts = config_data.get('scripts', [])
        
        all_success = True
        
        # Execute commands
        for cmd in commands:
            if not self.running:
                break
            
            result = self.execute_with_retry(cmd)
            self.results.append(result)
            
            if not result.success:
                all_success = False
                if config_data.get('stop_on_error', True):
                    break
        
        # Execute scripts
        for script_info in scripts:
            if not self.running:
                break
            
            script_path = script_info['path']
            args = script_info.get('args', [])
            
            result = self.execute_script(script_path, args)
            self.results.append(result)
            
            if not result.success:
                all_success = False
                if config_data.get('stop_on_error', True):
                    break
        
        return all_success
    
    def generate_report(self, output_file: Optional[str] = None) -> Dict[str, Any]:
        """Generates execution report"""
        total_commands = len(self.results)
        successful_commands = sum(1 for r in self.results if r.success)
        failed_commands = total_commands - successful_commands
        
        total_time = sum(r.execution_time for r in self.results)
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'summary': {
                'total_commands': total_commands,
                'successful_commands': successful_commands,
                'failed_commands': failed_commands,
                'success_rate': (successful_commands / total_commands * 100) if total_commands > 0 else 0,
                'total_execution_time': total_time
            },
            'resource_usage': self.resource_monitor.get_summary(),
            'results': [asdict(r) for r in self.results]
        }
        
        if output_file:
            with open(output_file, 'w') as f:
                json.dump(report, f, indent=2)
            self.logger.info(f"Report saved to: {output_file}")
        
        return report
    
    def print_summary(self):
        """Prints execution summary"""
        if not self.results:
            self.logger.info("No commands were executed")
            return
        
        successful = [r for r in self.results if r.success]
        failed = [r for r in self.results if not r.success]
        
        print("\n" + "="*60)
        print("📊 EXECUTION SUMMARY")
        print("="*60)
        print(f"Total commands: {len(self.results)}")
        print(f"✅ Successes: {len(successful)}")
        print(f"❌ Failures: {len(failed)}")
        print(f"📈 Success rate: {len(successful)/len(self.results)*100:.1f}%")
        
        if failed:
            print("\n❌ FAILED COMMANDS:")
            for result in failed:
                print(f"  - {result.command}")
                if result.error_message:
                    print(f"    Error: {result.error_message}")
        
        # Resources
        if self.config.monitor_resources:
            summary = self.resource_monitor.get_summary()
            print(f"\n💻 RESOURCE USAGE:")
            print(f"  Duration: {summary['duration']:.1f}s")
            print(f"  Peak CPU: {summary['peak_cpu']:.1f}%")
            print(f"  Peak memory: {summary['peak_memory']:.1f}%")
        
        print("="*60)


def main():
    """Main function"""
    parser = argparse.ArgumentParser(description="Python installer for shell scripts")
    parser.add_argument("--config", "-c", help="YAML configuration file")
    parser.add_argument("--script", "-s", help="Shell script to execute")
    parser.add_argument("--command", "-cmd", help="Shell command to execute")
    parser.add_argument("--timeout", "-t", type=int, default=300, help="Timeout in seconds")
    parser.add_argument("--retry", "-r", type=int, default=3, help="Number of retry attempts")
    parser.add_argument("--parallel", "-p", action="store_true", help="Parallel execution")
    parser.add_argument("--dry-run", "-d", action="store_true", help="Simulation mode")
    parser.add_argument("--log-file", "-l", help="Log file")
    parser.add_argument("--report", help="JSON report file")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose log")
    
    args = parser.parse_args()
    
    # Configuration
    config = InstallerConfig(
        timeout=args.timeout,
        retry_attempts=args.retry,
        parallel_execution=args.parallel,
        log_file=args.log_file,
        dry_run=args.dry_run,
        log_level="DEBUG" if args.verbose else "INFO"
    )
    
    installer = ShellInstaller(config)
    
    try:
        success = False
        
        if args.config:
            # Execute based on configuration file
            success = installer.install_from_config(args.config)
        elif args.script:
            # Execute script
            result = installer.execute_script(args.script)
            success = result.success
        elif args.command:
            # Execute command
            result = installer.execute_with_retry(args.command)
            success = result.success
        else:
            print("❌ Specify --config, --script or --command")
            return 1
        
        # Generate report
        if args.report:
            installer.generate_report(args.report)
        
        # Print summary
        installer.print_summary()
        
        return 0 if success else 1
        
    except KeyboardInterrupt:
        print("\n⚠️  Execution interrupted by user")
        return 1
    except Exception as e:
        print(f"💥 Unexpected error: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main()) 