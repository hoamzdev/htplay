from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
cache = Path('C:/Users/Admin/.gradle/caches')
lib = Path('C:/Program Files/Android/Android Studio/plugins/Kotlin/kotlinc/lib')
java = 'C:/Program Files/Android/Android Studio/jbr/bin/java.exe'
out = root / '.verification/back-test-classes'
out.mkdir(exist_ok=True)
cp = [lib / 'kotlin-stdlib.jar', lib / 'parcelize-runtime.jar',
      Path('C:/Users/Admin/AppData/Local/Android/Sdk/platforms/android-37.0/android.jar')]
cp += list((cache / '9.6.0/transforms').rglob('classes.jar'))
for group, artifact in [('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm'),
                        ('org.jetbrains.kotlinx', 'kotlinx-coroutines-test-jvm'),
                        ('junit', 'junit'), ('org.hamcrest', 'hamcrest-core'),
                        ('javax.inject', 'javax.inject'),
                        ('androidx.lifecycle', 'lifecycle-common-jvm'),
                        ('androidx.annotation', 'annotation-jvm')]:
    cp += [p for p in (cache / 'modules-2/files-2.1' / group / artifact).rglob('*.jar')
           if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
for module in ['app', 'domain', 'data']:
    cp.append(root / module / 'build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes')
tests = root / 'app/src/test/java/com/htdev/movie/htplay/viewmodel'
sources = [tests / name for name in ['FakeMovieRepository.kt', 'HomeViewModelTest.kt', 'ShowAllViewModelTest.kt']]
args = ['-no-stdlib', '-no-reflect', '-jvm-target', '11', '-classpath',
        ';'.join(map(str, cp)), '-d', str(out), *map(str, sources)]
# Use an argument file to avoid Windows' command-line length limit.
import json
argfile = root / '.verification/back-tests.args'
argfile.write_text('\n'.join(json.dumps(a) for a in args), encoding='utf-8')
subprocess.run([java, '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                '@' + str(argfile)], check=True)
subprocess.run([java, '-cp', ';'.join(map(str, [out, *cp])), 'org.junit.runner.JUnitCore',
                'com.htdev.movie.htplay.viewmodel.HomeViewModelTest',
                'com.htdev.movie.htplay.viewmodel.ShowAllViewModelTest'], check=True)
