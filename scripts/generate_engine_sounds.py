#!/usr/bin/env python3
"""Generate looping mono OGG engine samples for Peter Wolf's Planes."""

from __future__ import annotations

import array
import math
import os
import random
import struct
import subprocess
import tempfile
import wave


SR = 22050
DURATION = 3.2
OVERLAP = int(SR * 0.18)


def clamp(value: float, lo: float = -1.0, hi: float = 1.0) -> float:
	return lo if value < lo else hi if value > hi else value


def one_pole_lowpass(x: float, state: list[float], alpha: float) -> float:
	state[0] = state[0] + alpha * (x - state[0])
	return state[0]


def one_pole_highpass(x: float, state: list[float], alpha: float) -> float:
	y = alpha * (state[0] + x - state[1])
	state[0] = y
	state[1] = x
	return y


def crossfade_loop(samples: list[float], overlap: int) -> list[float]:
	n = len(samples)
	loop_len = n - overlap
	out = samples[:loop_len]
	for i in range(overlap):
		t = i / overlap
		# equal-power-ish blend of tail into head
		out[i] = samples[i] * t + samples[loop_len + i] * (1.0 - t)
	return out


def normalize(samples: list[float], peak: float = 0.89) -> list[float]:
	m = max(abs(s) for s in samples) or 1.0
	gain = peak / m
	return [s * gain for s in samples]


def synth_plane() -> list[float]:
	n = int(SR * DURATION) + OVERLAP
	rnd = random.Random(1701)
	noise = [rnd.uniform(-1.0, 1.0) for _ in range(n)]
	lp_exh = [0.0]
	lp_mech = [0.0]
	hp_air = [0.0, 0.0]
	out: list[float] = []
	for i in range(n):
		t = i / SR
		# Vintage radial: ~48 Hz exhaust pulses with uneven cylinders
		pulse = 0.0
		for k, phase in enumerate((0.0, 0.13, 0.29, 0.41, 0.58, 0.72, 0.87)):
			ang = (48.0 * t + phase) * 2.0 * math.pi
			p = 0.5 + 0.5 * math.sin(ang)
			pulse += (p ** 6) * (0.85 + 0.15 * math.sin(2.0 * math.pi * (k + 1) * 0.37 * t))
		pulse /= 3.4
		rumble = (
			0.62 * math.sin(2.0 * math.pi * 74.0 * t)
			+ 0.34 * math.sin(2.0 * math.pi * 148.0 * t + 0.35)
			+ 0.18 * math.sin(2.0 * math.pi * 37.0 * t + 1.1)
			+ 0.12 * math.sin(2.0 * math.pi * 222.0 * t + 0.7)
		)
		prop = (
			0.38 * math.sin(2.0 * math.pi * 88.0 * t)
			+ 0.20 * math.sin(2.0 * math.pi * 176.0 * t + 0.2)
			+ 0.10 * math.sin(2.0 * math.pi * 264.0 * t + 0.9)
		)
		exh = one_pole_lowpass(noise[i], lp_exh, 0.18)
		exhaust = exh * (0.25 + 0.75 * pulse)
		air = one_pole_highpass(noise[i], hp_air, 0.96) * 0.08
		mech = one_pole_lowpass(noise[i] * (pulse ** 2), lp_mech, 0.45) * 0.22
		s = rumble * (0.45 + 0.55 * pulse) + prop * (0.7 + 0.3 * pulse) + exhaust * 0.85 + air + mech
		out.append(s)
	return normalize(crossfade_loop(out, OVERLAP), 0.90)


def synth_helicopter() -> list[float]:
	n = int(SR * DURATION) + OVERLAP
	rnd = random.Random(42)
	noise = [rnd.uniform(-1.0, 1.0) for _ in range(n)]
	lp_rotor = [0.0]
	lp_body = [0.0]
	hp_tail = [0.0, 0.0]
	out: list[float] = []
	blade_hz = 8.2
	for i in range(n):
		t = i / SR
		# 4-blade whoosh: strong amplitude envelope
		blade = 0.5 + 0.5 * math.sin(2.0 * math.pi * blade_hz * t)
		chop = blade ** 2.4
		# slight blade inequality so it does not sound synthetic
		chop *= 0.92 + 0.08 * math.sin(2.0 * math.pi * (blade_hz / 4.0) * t)
		rotor_noise = one_pole_lowpass(noise[i], lp_rotor, 0.22)
		whoosh = rotor_noise * (0.18 + 0.82 * chop) * 1.15
		# low thump on each blade
		thump = (chop ** 3) * (
			0.55 * math.sin(2.0 * math.pi * 52.0 * t)
			+ 0.25 * math.sin(2.0 * math.pi * 26.0 * t)
		)
		turbine = (
			0.22 * math.sin(2.0 * math.pi * 286.0 * t)
			+ 0.12 * math.sin(2.0 * math.pi * 572.0 * t + 0.4)
			+ 0.07 * math.sin(2.0 * math.pi * 143.0 * t)
		)
		body = one_pole_lowpass(noise[i], lp_body, 0.08) * 0.28
		tail = one_pole_highpass(math.sin(2.0 * math.pi * 510.0 * t) * 0.10 + noise[i] * 0.04, hp_tail, 0.97)
		s = whoosh * 0.95 + thump * 0.85 + turbine * (0.75 + 0.25 * chop) + body + tail * 0.55
		out.append(s)
	return normalize(crossfade_loop(out, OVERLAP), 0.88)


def write_wav(path: str, samples: list[float]) -> None:
	with wave.open(path, "w") as wav:
		wav.setnchannels(1)
		wav.setsampwidth(2)
		wav.setframerate(SR)
		frames = bytearray()
		for sample in samples:
			frames.extend(struct.pack("<h", int(clamp(sample) * 32767.0)))
		wav.writeframes(frames)


def find_ffmpeg() -> str:
	for candidate in (
		os.environ.get("FFMPEG"),
		"ffmpeg",
		"/opt/homebrew/bin/ffmpeg",
		"/usr/local/bin/ffmpeg",
	):
		if candidate and os.path.isfile(candidate) and os.access(candidate, os.X_OK):
			return candidate
	try:
		import imageio_ffmpeg

		return imageio_ffmpeg.get_ffmpeg_exe()
	except Exception:
		pass
	raise FileNotFoundError("ffmpeg not found; install ffmpeg or imageio-ffmpeg")


def wav_to_ogg(wav_path: str, ogg_path: str) -> None:
	subprocess.check_call(
		[
			find_ffmpeg(),
			"-y",
			"-hide_banner",
			"-loglevel",
			"error",
			"-i",
			wav_path,
			"-c:a",
			"libvorbis",
			"-q:a",
			"5",
			ogg_path,
		]
	)


def main() -> None:
	root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
	out_dir = os.path.join(root, "src", "main", "resources", "assets", "peterwolfs_planes", "sounds", "engine")
	os.makedirs(out_dir, exist_ok=True)
	with tempfile.TemporaryDirectory() as tmp:
		plane_wav = os.path.join(tmp, "plane.wav")
		heli_wav = os.path.join(tmp, "helicopter.wav")
		write_wav(plane_wav, synth_plane())
		write_wav(heli_wav, synth_helicopter())
		wav_to_ogg(plane_wav, os.path.join(out_dir, "plane.ogg"))
		wav_to_ogg(heli_wav, os.path.join(out_dir, "helicopter.ogg"))
	print("Wrote engine loops to", out_dir)


if __name__ == "__main__":
	main()
