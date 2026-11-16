from pathlib import Path
import json
import subprocess
import re
import zipfile

root = Path(__file__).resolve().parents[1]
args = [json.loads(line) for line in (root / '.verification/back-tests.args').read_text(encoding='utf-8').splitlines()]
cp = args[args.index('-classpath') + 1]
cp = ';'.join(p for p in cp.split(';') if 'kotlinx-coroutines-' not in p or '1.11.0' in p)
latest = {}
other = []
for entry in cp.split(';'):
    parts = Path(entry).parts
    if 'transformed' not in parts:
        other.append(entry)
        continue
    label = parts[parts.index('transformed') + 1]
    match = re.match(r'(.+?)-(\d.*)', label)
    if not match:
        continue
    name, version = match.groups()
    key = tuple(map(int, re.findall(r'\d+', version)))
    if name not in latest or key > latest[name][0]:
        latest[name] = (key, entry)
cp = ';'.join([*other, *[entry for _, entry in latest.values()]])
prefix = []
cache = Path('C:/Users/Admin/.gradle/caches/modules-2/files-2.1')
for group, name, version in [('androidx.lifecycle', 'lifecycle-viewmodel-android', '2.11.0'),
                             ('androidx.lifecycle', 'lifecycle-viewmodel-savedstate-android', '2.11.0'),
                             ('androidx.paging', 'paging-common-android', '3.5.3')]:
    aar = next((cache / group / name / version).rglob('*.aar'))
    target = root / '.verification' / (name + '-back-test.jar')
    with zipfile.ZipFile(aar) as archive:
        target.write_bytes(archive.read('classes.jar'))
    prefix.append(str(target))
cp = ';'.join([*prefix, cp])
classes = args[args.index('-d') + 1]
runtime = ['-cp', classes + ';' + cp, 'org.junit.runner.JUnitCore',
           'com.htdev.movie.htplay.viewmodel.HomeViewModelTest',
           'com.htdev.movie.htplay.viewmodel.ShowAllViewModelTest']
argfile = root / '.verification/back-tests-java.args'
argfile.write_text('\n'.join(json.dumps(a) for a in runtime), encoding='utf-8')
subprocess.run(['C:/Program Files/Android/Android Studio/jbr/bin/java.exe', '@' + str(argfile)], check=True)
