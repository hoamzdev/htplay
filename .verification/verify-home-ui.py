from pathlib import Path
import subprocess
import zipfile
import re
import json

root = Path('E:/movie')
cache = Path('C:/Users/Admin/.gradle/caches/modules-2/files-2.1')
lib = Path('C:/Program Files/Android/Android Studio/plugins/Kotlin/kotlinc/lib')
jdk = Path('C:/Program Files/Android/Android Studio/jbr/bin')
out = root / '.verification/home'

def run(args):
    result = subprocess.run(list(map(str, args)), text=True, capture_output=True)
    print(result.stdout, result.stderr, flush=True)
    if result.returncode: raise SystemExit(result.returncode)

def binary(group, artifact, version=None):
    folder = cache / group / artifact
    if version:
        folder /= version
    files = [p for p in folder.rglob('*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
    return files[0] if files else None

cp = [lib / 'kotlin-stdlib.jar']
deps = [('androidx.databinding','databinding-compiler-common','9.4.1'),
        ('androidx.databinding','databinding-common','9.4.1'),
        ('com.android.databinding','baseLibrary','9.4.1'),
        ('com.android.tools','annotations','32.4.1'),
        ('com.android.tools.build.jetifier','jetifier-core',None),
        ('com.google.code.gson','gson','2.15.0'),
        ('com.google.guava','guava',None),
        ('com.googlecode.juniversalchardet','juniversalchardet',None),
        ('com.squareup','javapoet','1.13.0'), ('commons-io','commons-io',None)]
for args in deps:
    found = binary(*args)
    if found: cp.append(found)
for group in ['org.glassfish.jaxb', 'javax.xml.bind', 'jakarta.xml.bind', 'javax.activation', 'jakarta.activation', 'org.jvnet.staxex', 'com.sun.istack', 'com.sun.xml.fastinfoset', 'com.google.guava', 'org.antlr']:
    for p in (cache / group).rglob('*.jar'):
        if not p.name.endswith(('-sources.jar','-javadoc.jar')): cp.append(p)
generator_classes = out / 'generator-classes'
generator_classes.mkdir(exist_ok=True)
run([jdk/'java.exe','-cp',lib/'*','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
     '-no-stdlib','-no-reflect','-classpath',';'.join(map(str,cp)),
     '-d',generator_classes,out/'GenerateBindings.kt'])
run([jdk/'java.exe','-cp',';'.join(map(str,[generator_classes,*cp])),
     'GenerateBindingsKt',root/'app/src/main/res/layout',out/'bindings'])

sdk = Path('C:/Users/Admin/AppData/Local/Android/Sdk')
run([sdk/'build-tools/37.0.0/aapt2.exe','link','--static-lib','--merge-only',
     '--auto-add-overlay','--manifest',root/'app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml',
     '-I',sdk/'platforms/android-37.0/android.jar','-R',out/'resources.zip',
     '--java',out/'r-java','-o',out/'resources.ap_'])

transforms = Path('C:/Users/Admin/.gradle/caches/9.6.0/transforms')
artifacts = {}
for p in transforms.rglob('classes.jar'):
    relative = p.relative_to(transforms)
    if len(relative.parts) < 4 or relative.parts[1] != 'transformed': continue
    label = relative.parts[2]
    match = re.match(r'(.+?)-(\d.*)',label)
    if not match: continue
    name, version = match.groups()
    key = tuple(map(int,re.findall(r'\d+',version)))
    if name not in artifacts or key > artifacts[name][0]: artifacts[name] = (key,p)
ui_cp = [out/'classes', sdk/'platforms/android-37.0/android.jar',lib/'kotlin-stdlib.jar',
         *[p for _,p in artifacts.values()]]
for group,artifact,version in [
    ('javax.inject','javax.inject','1'), ('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.11.0'),
    ('org.jetbrains.kotlinx','kotlinx-coroutines-android','1.11.0'),
    ('androidx.lifecycle','lifecycle-common-jvm','2.11.0'),
    ('androidx.annotation','annotation-jvm','1.10.0'), ('com.google.dagger','dagger',None),
    ('com.google.code.gson','gson','2.15.0'), ('com.squareup.retrofit2','retrofit','3.0.0'),
]:
    found=binary(group,artifact,version)
    if found: ui_cp.append(found)
for module in ['app','data','domain']:
    ui_cp.append(root/module/'build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes')
ui_classes = out/'ui-classes'
ui_classes.mkdir(exist_ok=True)
java_files = list((out/'bindings').rglob('*.java')) + list((out/'r-java').rglob('*.java'))
java_args=['-classpath',';'.join(map(str,ui_cp)),'-d',str(ui_classes),*map(str,java_files)]
(out/'javac.args').write_text('\n'.join(json.dumps(s) for s in java_args), encoding='utf-8')
run([jdk/'javac.exe','@'+str(out/'javac.args')])

app=root/'app/src/main/java/com/htdev/movie/htplay'
sources=[app/'MainActivity.kt',app/'adapter/HomeAdapter.kt',app/'adapter/HomeCategoryAdapter.kt',
         app/'adapter/MovieAdapter.kt', app/'favorite/ChooseFavoritesActivity.kt',
         app/'favorite/ChooseFavoriteAdapter.kt',app/'util/GenreLocalStrings.kt']
kotlin_args=['-no-stdlib','-no-reflect','-jvm-target','11','-classpath',
             ';'.join(map(str,[ui_classes,*ui_cp])),'-d',str(ui_classes),*map(str,sources)]
(out/'kotlinc-ui.args').write_text('\n'.join(json.dumps(s) for s in kotlin_args),encoding='utf-8')
run([jdk/'java.exe','-cp',lib/'*','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','@'+str(out/'kotlinc-ui.args')])
print('UI Kotlin compiled against generated ViewBinding classes and resources.')
