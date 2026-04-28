<div align="center">

# VoraRTP

**Folia destekli, akıllı ve hafif bir Random Teleport eklentisi.**

Oyuncularını güvenle haritanın dört bir yanına ışınla — lag olmadan, crash olmadan.

[![Folia](https://img.shields.io/badge/Folia-1.21.11-brightgreen?style=flat-square)](https://github.com/PaperMC/Folia)
[![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)](LICENSE)

</div>

---

## Neden VoraRTP?

Piyasadaki çoğu RTP eklentisi Folia ile ya hiç çalışmaz ya da sunucuyu kararsız hale getirir. VoraRTP sıfırdan Folia gözetilerek yazıldı. Bölgesel thread sistemi tam anlamıyla destekleniyor; chunk yükleme async yapılıyor, block erişimi doğru region thread üzerinde gerçekleşiyor, hiçbir yerde `Bukkit.getScheduler()` kullanılmıyor.

Bunun yanında **konum önbellekleme sistemi** sayesinde oyuncu `/rtp` yazdığında güvenli bir konum zaten hazır bekliyor. Bekleme yok, gecikme yok.

---

## Özellikler

**Teknik taraf**

- Folia 1.21.11 uyumlu — `RegionScheduler`, `AsyncScheduler` ve `player.getScheduler()` doğru kullanılmıştır
- Chunk'lar main thread'i bloklamadan async yüklenir, timeout koruması mevcuttur
- Block erişimi her zaman doğru region thread'inde gerçekleşir
- `ConcurrentHashMap` ve `ConcurrentLinkedQueue` ile thread-safe yapı

**Oyun içi**

- Overworld, Nether ve End ayrı ayrı yapılandırılabilir
- Güvenli konumlar arka planda periyodik olarak önceden hesaplanır ve önbellekte tutulur
- Cooldown sistemi — oyuncuların RTP'yi spam yapmasını engeller, bypass izni desteklenir
- Teleport öncesi geri sayım — hareket eden oyuncunun işlemi otomatik iptal edilir
- Lav, ateş, kaktüs, magma, powder snow ve daha fazlasına karşı akıllı güvenli konum kontrolü
- Nether ve End için özel ek güvenlik kontrolleri
- Sezgisel GUI menüsü ile dünya seçimi
- Devre dışı bırakılan dünyalar menüde kilitli olarak gösterilir
- Her aşama için ayrı ses ve ekran başlığı yapılandırması
- `/rtp reload` — sunucu kapatmadan config güncelleme

---

## Gereksinimler

- Folia `1.21.11`
- Java `21+`

---

## Kurulum

1. `VoraRTP.jar` dosyasını sunucunun `plugins/` klasörüne koy
2. Sunucuyu başlat — `config.yml` otomatik oluşur
3. `plugins/VoraRTP/config.yml` dosyasını ihtiyacına göre düzenle
4. `/rtp reload` komutuyla değişiklikleri uygula

---

## Komutlar

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/rtp` | RTP menüsünü açar | `vora.rtp.use` |
| `/rtp reload` | Config dosyasını yeniden yükler | `vora.rtp.reload` |

---

## İzinler

| İzin | Varsayılan | Açıklama |
|------|------------|----------|
| `vora.rtp.use` | Herkes | `/rtp` komutunu kullanabilir |
| `vora.rtp.reload` | OP | Config reload yapabilir |
| `vora.rtp.bypass.cooldown` | OP | Cooldown'u atlar |

---

## Yapılandırma

Aşağıda `config.yml` dosyasının tüm seçenekleri açıklamalarıyla verilmiştir.

```yaml
# Mesaj öneki
prefix: "&8❖ &fᴠᴏʀᴀ &8» "

messages:
  player-only: "&fBu komut sadece oyuncular içindir."
  no-permission: "&cBu komutu kullanma iznin yok."
  busy: "&cZaten bir RTP işlemi devam ediyor."
  cooldown: "&cRTP kullanmak için &6{seconds} &csaniye bekle."
  countdown-cancelled: "&cHareket ettin, RTP iptal edildi."
  teleporting: "&fRTP başlatılıyor..."
  no-world: "&cHedef dünya bulunamadı."
  failed: "&cGüvenli bir konum bulunamadı."
  teleport-failed: "&cRTP başarısız oldu."
  success: "&aRTP tamamlandı."
  reloaded: "&aConfig yeniden yüklendi."
  world-disabled: "&cBu dünya kapalı."

cooldown:
  seconds: 5                           # 0 = devre dışı
  bypass-permission: "vora.rtp.bypass.cooldown"

countdown:
  seconds: 5                           # 0 = geri sayım olmadan anında ışınla
  cancel-on-move: true                 # Hareket edince iptal et

teleport:
  max-attempts: 30                     # Güvenli konum bulamazsa kaç kez denesin
  chunk-timeout-ms: 10000              # Chunk yükleme zaman aşımı (ms)

  location-cache:
    enabled: true
    size-per-world: 5                  # Her dünya için önbellekte tutulacak konum sayısı
    refill-interval-ticks: 600         # Önbellek ne sıklıkta yenilensin (tick)

  overworld:
    min-distance: 200                  # Spawn'dan minimum uzaklık (blok)
    max-distance: 5000                 # Spawn'dan maksimum uzaklık (blok)
    search-depth: 8                    # Güvenli zemin ararken yukarıdan kaç blok insin

  nether:
    min-distance: 100
    max-distance: 2000
    min-y: 24                          # Aranacak minimum Y seviyesi
    max-y: 118                         # Aranacak maksimum Y seviyesi

  end:
    min-distance: 200
    max-distance: 3000
    search-depth: 8
    support-check-depth: 8             # Ada zemini kontrolü için derinlik
    min-supported-columns: 2           # Güvenli sayılması için minimum destek sütunu

  # Oyuncunun ayakta duramayacağı bloklar (baş/ayak alanı kontrolü)
  dangerous-materials:
    - LAVA
    - FIRE
    - SOUL_FIRE
    - MAGMA_BLOCK
    - CACTUS
    - SWEET_BERRY_BUSH
    - WITHER_ROSE
    - POWDER_SNOW
    - CAMPFIRE
    - SOUL_CAMPFIRE
    - POINTED_DRIPSTONE

  # Oyuncunun üstüne basamayacağı bloklar (zemin kontrolü)
  unsafe-ground-materials:
    - WATER
    - LAVA
    - FIRE
    - SOUL_FIRE
    - MAGMA_BLOCK
    - CACTUS

  # Oyuncunun yakınında olmaması gereken bloklar (çevre kontrolü)
  unsafe-near-materials:
    - LAVA
    - FIRE
    - SOUL_FIRE
    - MAGMA_BLOCK
```

---

## Nasıl Çalışır?

Oyuncu `/rtp` yazdığında önce GUI menüsü açılır. Bir dünya seçtiğinde geri sayım başlar. Geri sayım boyunca hareket ederse işlem iptal edilir. Süre dolduğunda eklenti hedef dünyada güvenli bir konum arar — önce önbellekten bakar, hazır konum varsa anında ışınlar, yoksa async olarak yeni bir konum hesaplar.

Konum bulma sürecinde chunk'lar asenkron yüklenir, ardından block kontrolü ilgili region thread'inde yapılır. Bulunan konum lav, ateş, magma gibi tehlikeli bloklardan uzak ve oyuncunun iki blok yüksekliğinde durabilmesine uygun olmak zorundadır.

---

## Geliştirici

**gorluk11** — Hata bildirimi veya öneri için Issues sekmesini kullanabilirsiniz.

---

## Lisans

Bu proje MIT lisansı ile dağıtılmaktadır. Ayrıntılar için `LICENSE` dosyasına bakın.