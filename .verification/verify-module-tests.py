from pathlib import Path
import subprocess, zipfile
root = Path('E:/movie')
cache = Path('C:/Users/Admin/.gradle/caches/modules-2/files-2.1')
lib = Path('C:/Program Files/Android/Android Studio/plugins/Kotlin/kotlinc/lib')
java = 'C:/Program Files/Android/Android Studio/jbr/bin/java.exe'
def jar(group, name, version):
    return next((cache/group/name/version).rglob(name+'-'+version+'.jar'))
cp = [lib/'kotlin-stdlib.jar', lib/'parcelize-runtime.jar', Path('C:/Users/Admin/AppData/Local/Android/Sdk/platforms/android-37.0/android.jar'), jar('com.google.code.gson','gson','2.15.0'), jar('junit','junit','4.13.2'), jar('org.hamcrest','hamcrest-core','1.3'), jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.11.0'), jar('org.jetbrains.kotlinx','kotlinx-coroutines-test-jvm','1.11.0')]
files = [root/'domain/src/main/java/com/htdev/movie/htplay/domain/entities/MovieList.kt',root/'domain/src/main/java/com/htdev/movie/htplay/domain/entities/GenreLocal.kt',root/'domain/src/main/java/com/htdev/movie/htplay/domain/common/Result.kt',root/'domain/src/test/java/com/htdev/movie/htplay/domain/common/ResultTest.kt',root/'data/src/main/java/com/htdev/movie/htplay/data/common/Api.kt',root/'data/src/main/java/com/htdev/movie/htplay/data/common/ImageUrls.kt',root/'data/src/main/java/com/htdev/movie/htplay/data/db/DataGenreLocal.kt',root/'data/src/test/java/com/htdev/movie/htplay/data/entities/dto/MovieMappingTest.kt']
files += list((root/'data/src/main/java/com/htdev/movie/htplay/data/entities/dto').glob('Movie*.kt'))
output = root/'.verification/module-unit-classes'
output.mkdir(exist_ok=True)
cmd = [java,'-cp',str(lib/'*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','11','-classpath',';'.join(map(str,cp)),'-Xplugin='+str(lib/'parcelize-compiler.jar'),'-d',str(output),*map(str,files)]
r = subprocess.run(cmd, text=True, capture_output=True)
print(r.stdout, r.stderr)
if r.returncode: raise SystemExit(r.returncode)
r = subprocess.run([java,'-cp',';'.join(map(str,[output,*cp])),'org.junit.runner.JUnitCore','com.htdev.movie.htplay.data.entities.dto.MovieMappingTest','com.htdev.movie.htplay.domain.common.ResultTest'],text=True,capture_output=True)
print(r.stdout,r.stderr)
raise SystemExit(r.returncode)

