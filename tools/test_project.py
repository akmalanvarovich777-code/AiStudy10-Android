from pathlib import Path
import re, json, subprocess, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
m=root/'app/src/main/AndroidManifest.xml'
xml=ET.parse(m).getroot()
ns='{http://schemas.android.com/apk/res/android}'
activities=xml.findall('./application/activity')
profiles=[a for a in activities if 'Profile' in a.get(ns+'name','')]
assert len(profiles)==10
assert len(set(p.get(ns+'process') for p in profiles))==10
for p in profiles:
    assert p.get(ns+'exported')=='false'
assert (root/'app/src/main/res/drawable-nodpi/ic_aistudy.png').exists()
db=json.loads((root/'app/src/main/assets/answers.json').read_text())
assert len(db)>=35
for p in db:assert isinstance(p['question'],str) and isinstance(p['answer'],str)
js=root/'app/src/main/assets/companion.js'
subprocess.run(['node','--check',str(js)],check=True)
for n in range(1,11):
    t=(root/f'app/src/main/java/uz/akmal/aistudy10/Profile{n:02d}Activity.java').read_text()
    assert 'return '+str(n)+';' in t
src=(root/'app/src/main/java/uz/akmal/aistudy10/ProfileActivity.java').read_text()
assert 'WebView.setDataDirectorySuffix' in src
assert 'setMediaPlaybackRequiresUserGesture(false)' in src
assert 'omp.aistudy.uz' in src
assert 'window.__AIS_ANDROID_ANSWERS=' in src
assert 'Muallif: Akmal Muxamedjanov' in src
print('OK: 10 profillar, alohida process, assetlar, JS sintaksisi va Android manba qoidalari. APK yig‘ilmagan.')
