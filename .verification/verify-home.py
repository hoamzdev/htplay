from pathlib import Path
import subprocess
import zipfile

root = Path('E:/movie')
cache = Path('C:/Users/Admin/.gradle/caches/modules-2/files-2.1')
lib = Path('C:/Program Files/Android/Android Studio/plugins/Kotlin/kotlinc/lib')
java = 'C:/Program Files/Android/Android Studio/jbr/bin/java.exe'
out = root / '.verification/home'
out.mkdir(exist_ok=True)

def dependency(group, artifact, version):
    folder = cache / group / artifact / version
    binaries = [p for p in folder.rglob('*') if p.suffix in ('.jar', '.aar') and not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
    binary = next(iter(binaries))
    if binary.suffix == '.aar':
        target = out / f'{artifact}-{version}.jar'
        with zipfile.ZipFile(binary) as archive:
            target.write_bytes(archive.read('classes.jar'))
        return target
    return binary

cp = [lib / 'kotlin-stdlib.jar', lib / 'parcelize-runtime.jar',
      Path('C:/Users/Admin/AppData/Local/Android/Sdk/platforms/android-37.0/android.jar')]
for args in [
    ('javax.inject', 'javax.inject', '1'),
    ('com.google.dagger', 'hilt-android', '2.60.1'),
    ('androidx.lifecycle', 'lifecycle-viewmodel-android', '2.11.0'),
    ('androidx.lifecycle', 'lifecycle-common-jvm', '2.11.0'),
    ('androidx.paging', 'paging-common-android', '3.5.3'),
    ('androidx.annotation', 'annotation-jvm', '1.10.0'),
    ('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.11.0'),
    ('org.jetbrains.kotlinx', 'kotlinx-coroutines-test-jvm', '1.11.0'),
    ('junit', 'junit', '4.13.2'),
    ('org.hamcrest', 'hamcrest-core', '1.3'),
]:
    cp.append(dependency(*args))

domain = root / 'domain/src/main/java/com/htdev/movie/htplay/domain'
app = root / 'app/src/main/java/com/htdev/movie/htplay'
files = [domain / 'common/Result.kt', domain / 'entities/GenreLocal.kt', domain / 'entities/MovieList.kt',
         domain / 'repository/MovieRepository.kt', domain / 'repository/LocalStorageRepository.kt',
         root / 'data/src/main/java/com/htdev/movie/htplay/data/db/DataGenreLocal.kt',
         root / 'data/src/main/java/com/htdev/movie/htplay/data/common/Api.kt',
         app / 'viewmodel/MainViewModel.kt', app / 'favorite/FavoriteViewModel.kt', app / 'util/HomeSections.kt']
files += list((domain / 'usecase/movie').rglob('*.kt'))
files += list((app / 'entities').glob('*.kt'))
tests = root / 'app/src/test/java/com/htdev/movie/htplay'
files += [tests / 'util/HomeSectionsTest.kt', *list((tests / 'viewmodel').glob('*.kt'))]
classes = out / 'classes'
classes.mkdir(exist_ok=True)
cmd = [java, '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
       '-no-stdlib', '-no-reflect', '-jvm-target', '11', '-classpath', ';'.join(map(str, cp)),
       '-Xplugin=' + str(lib / 'parcelize-compiler.jar'), '-d', str(classes), *map(str, files)]
result = subprocess.run(cmd, text=True, capture_output=True)
print(result.stdout, result.stderr)
if result.returncode:
    raise SystemExit(result.returncode)
result = subprocess.run([java, '-cp', ';'.join(map(str, [classes, *cp])), 'org.junit.runner.JUnitCore',
    'com.htdev.movie.htplay.util.HomeSectionsTest', 'com.htdev.movie.htplay.viewmodel.MainViewModelTest'],
    text=True, capture_output=True)
print(result.stdout, result.stderr)
raise SystemExit(result.returncode)
