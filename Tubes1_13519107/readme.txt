TUGAS BESAR 1 IF 2211 Strategi Algoritma

Pemanfaatan Algoritma Greedy dalam Aplikasi Permainan "Worms"

I. Identitas Pembuat

Nama Kelompok : gg ("good game")
 --------------------------------------------------------
|		Nama		  |	NIM	|  Kelas |
|---------------------------------|-------------|--------|
|  Daffa Ananda Pratama Resyaly   |   13519107  |   02   |
|      I Gede Govindabhakta       |   13519139	|   02   |
|      Stefanus Jeremy Aslan      |   13519175  |   02   |
 --------------------------------------------------------

II. Requirements

1. Install Java SE Development Kit (minimal versi 8)
Dapat di-instal melalui link:
https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html

Langkah untuk melakukan instalasi JDK untuk Windows, Mac OS, dan Ubuntu dapat dilihat pada link:
https://www3.ntu.edu.sg/home/ehchua/programming/howto/JDK_Howto.html

2. Install IntelliJ IDEA
Dapat di-instal melalui link:
https://www.jetbrains.com/idea/

3. Install NODEJS
Dapat di-instal melalui link:
https://nodejs.org/en/download/

III. Inisialisasi Program

Inisialisasi bot
 1. Buka file 'game-runner-config.json' pada folder "starter-pack"
 2. Ubah setting untuk "player-a" dan "player-b" menjadi sebagai berikut:
 "player-a": "./starter-bots/java"
 "player-b": "./reference-bot/javascript"

Inisialisasi program
A. Windows
 1. Buka file 'pom.xml' pada folder 'java' melalui IntelliJ IDEA
 2. Klik kanan pada windows file tersebut dan klik "Add as Maven Project"
 3. Pilih grup 'java-sample-bot'
 4. Pilih grup 'Lifecycle'
 5. Pilih 'install'

B. Linux
 1. Install Maven
 Gunakan command `sudo apt-get install mvn`

IV. Cara Menjalankan Program

A. Windows
 -> Alternatif 1 : Klik dua kali pada file 'run.bat' pada folder  "starter-pack"
 -> Alternatif 2 :
    1. Buka command prompt (cmd)
    2. Change directory ke folder "starter-pack" (`cd ..\starter-pack`)
    3. Jalankan command `run.bat`

B. Linux
 1. Buka shell di folder atau change directory ke folder 
 2. Jalankan command :
 `mvn install`
 `make run`

V. Inisialisasi dan Cara Penggunaan Visualizer
Visualizer digunakan untuk menampilkan GUI dari hasil permainan

Inisialisasi Visualizer
 Melalui web browser
  1. Download visualizer melalui link :
  https://github.com/dlweatherhead/entelect-challenge-2019-   visualiser/releases/tag/v1.0f1
  (Download file "EC2019.Final.v1.0f1.zip")
  2. Extract file tersebut pada directory yang diinginkan
 Melalui git bash
  1. Change directory ke tempat yang diinginkan untuk menaruh folder   visualizer
  2. Jalankan command `git clone https://github.com/dlweatherhead/entelect-challenge-2019-visualiser/releases/tag/v1.0f1`

Cara Penggunaan Visualizer
 1. Buka folder "match-logs" pada folder "starter-pack"
 2. Copy semua folder yang terdapat di dalam folder "match-logs"
 3. Paste semua folder tadi ke dalam folder "Matches" yang berada di dalam folder "EC2019 Final v1.0f1" (Folder visualizer yang telah diekstrak)