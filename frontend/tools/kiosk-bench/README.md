# Kiosk bench

Scripts for measuring the TMAG kiosk's map flights the way the requirements are stated (see
`frontend/docs/tmag-kiosk.md`): the release build, exactly 3840×2160 at DPR 1, 60 Hz, on
integrated graphics, with 6–8 stories, tapped with trusted input. They drive a separate test
Chrome through the DevTools Protocol. They need Node 22 or later and nothing else.

| Script | What it does |
|---|---|
| `hold.mjs` | Holds the page at 3840×2160, DPR 1, and optionally serves a stories file. Leave it running. |
| `check.mjs` | Checks the page is fit to measure: size, DPR, visible, 60 Hz. |
| `make-stories.mjs` | Builds a stories file: the real stories plus test stories made from existing save states (read-only). |
| `warmup.mjs` | Watches the warm-up until everything's cached and decoded; `--reload` times it from cold. |
| `flights.mjs` | Taps through the stories and reports every flight; `--repeat` and `--pan` for specific cases. |
| `run.mjs` | Runs a script file in the page, or takes a screenshot. |

## Set-up

1. Build and serve the release page (see DOCKER.md):
   `docker compose exec frontend yarn release-local`, then the page is
   `http://localhost:3451/tmag-kiosk-release.html`. The dev page (`tmag-kiosk.html`) is only
   for `make-stories.mjs`: never judge performance on it.
2. Set the primary display to 60 Hz. Chrome takes its frame rate from the primary display
   wherever its window is, and at 30 Hz every frame reads 33 ms.
3. Start a separate Chrome for testing, with its own profile. On a machine with a discrete GPU,
   `--force_low_power_gpu` puts it on the integrated one. In Git Bash on Windows:

   ```sh
   "/c/Program Files/Google/Chrome/Application/chrome.exe" \
     --user-data-dir="$TEMP/kiosk-bench-profile" --no-first-run --no-default-browser-check \
     --remote-debugging-port=9333 --force_low_power_gpu --use-angle=d3d11 \
     --force-device-scale-factor=1 \
     --disable-features=CalculateNativeWinOcclusion --disable-backgrounding-occluded-windows \
     --disable-renderer-backgrounding \
     --window-size=1600,1000 http://localhost:3451/tmag-kiosk-release.html
   ```

   The window can be any size: `hold.mjs` sets the page's size. The last three flags keep
   Chrome drawing while the window is covered (otherwise the page goes hidden and every
   measurement stalls), so it can sit behind your other windows. Don't run it uncapped
   (`--disable-gpu-vsync --disable-frame-rate-limit`): that floods the GPU and some draws
   stall for seconds, which tells you nothing about the kiosk.

## Measuring

```sh
cd frontend/tools/kiosk-bench

# Once, against the dev page: the real stories plus 7 test stories.
node make-stories.mjs --page tmag-kiosk.html --out stories.json

# In a terminal of its own, against the release page:
node hold.mjs --page tmag-kiosk-release --stories stories.json

node warmup.mjs --page tmag-kiosk-release --reload   # cold warm-up, until done
node check.mjs  --page tmag-kiosk-release            # should say "Ready to measure."
node flights.mjs --page tmag-kiosk-release           # every story, step, Back and home
```

Other cases:

```sh
node flights.mjs --page tmag-kiosk-release --pause 0          # taps the instant each flight lands
node flights.mjs --page tmag-kiosk-release --repeat 5,2,4     # story 5: step 2 <-> 3, four times
node flights.mjs --page tmag-kiosk-release --pan              # drag the home map, then open a story and tap Begin
```

## Reading the results

- **missed**: frame intervals over 25 ms, ie a missed vsync at 60 Hz (about 33 ms). The
  requirement is none. Intervals of 16.7 ± 0.6 ms are normal.
- **draw**: median/worst script time to draw a frame, against the 16.6 ms budget. **slow draws**
  lists each draw over 8 ms as [t, ms], t being the fraction of the flight, which shows where
  in the flight the cost is (eg t ≈ 0.02 is the first frame).
- **tap->frame**: from the pointerdown to the flight's first moving frame. It includes the
  script's own gap between press and release.
- **ready / UNPLANNED, tiles missing**: whether the tap found its flight planned and its tiles
  decoded. With `--pause` of 1000 or more, every flight should be ready.
- In the page's console, `imas_seamap.tmag_kiosk.flight.stats()`, `.bitmaps.stats()`,
  `.tile_cache.stats()` and `.core.warm_up_status()` give the same figures by hand.
