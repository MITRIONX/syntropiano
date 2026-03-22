#!/usr/bin/env python3
"""
MIDI to JSON converter for SyntroPiano.
Parses standard MIDI files and outputs JSON song data compatible with the app format.
"""

import mido
import json
import os
import sys

# Note name mapping
NOTE_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']

def midi_note_to_name(midi_note):
    """Convert MIDI note number (0-127) to pitch name like 'C4', 'F#3'."""
    octave = (midi_note // 12) - 1
    note_name = NOTE_NAMES[midi_note % 12]
    return f"{note_name}{octave}"

def determine_hand(midi_note, channel):
    """
    Determine hand assignment.
    - If channel info clearly separates hands (e.g., channel 0 vs 1), use that.
    - Otherwise, use pitch split: notes below middle C (MIDI 60) = Left, >= 60 = Right.
    """
    # Middle C = MIDI note 60
    if midi_note < 60:
        return "L"
    else:
        return "R"

def determine_hand_by_channel(channel):
    """
    For multi-channel MIDI files, channel-based hand assignment.
    Typically channel 0 = right hand (treble), channel 1 = left hand (bass).
    """
    if channel == 0:
        return "R"
    elif channel == 1:
        return "L"
    return None  # Unknown, fall back to pitch split

def parse_midi_file(filepath):
    """Parse a MIDI file and extract note data."""
    mid = mido.MidiFile(filepath)

    print(f"\n--- Analyzing: {os.path.basename(filepath)} ---")
    print(f"  Type: {mid.type}")
    print(f"  Ticks per beat: {mid.ticks_per_beat}")
    print(f"  Number of tracks: {len(mid.tracks)}")

    # Extract tempo and time signature from all tracks
    tempo = 500000  # Default: 120 BPM (microseconds per beat)
    time_sig_num = 4
    time_sig_den = 4

    # Collect all tempo changes and time signature changes
    tempo_events = []
    time_sig_events = []

    # First pass: find total length in ticks
    total_ticks = 0
    for i, track in enumerate(mid.tracks):
        abs_time = 0
        print(f"  Track {i}: '{track.name}' ({len(track)} messages)")
        for msg in track:
            abs_time += msg.time
            if msg.type == 'set_tempo':
                tempo_events.append((abs_time, msg.tempo))
                print(f"    Tempo event at tick {abs_time}: {mido.tempo2bpm(msg.tempo):.1f} BPM")
            elif msg.type == 'time_signature':
                time_sig_events.append((abs_time, msg.numerator, msg.denominator))
                print(f"    Time signature at tick {abs_time}: {msg.numerator}/{msg.denominator}")
        total_ticks = max(total_ticks, abs_time)

    # Use the first tempo event (or default)
    if tempo_events:
        tempo = tempo_events[0][1]

    # Determine the main time signature (the one that covers the most ticks)
    if time_sig_events:
        if len(time_sig_events) == 1:
            time_sig_num = time_sig_events[0][1]
            time_sig_den = time_sig_events[0][2]
        else:
            # Calculate duration each time signature covers
            best_sig = None
            best_duration = 0
            for idx, (tick, num, den) in enumerate(time_sig_events):
                if idx + 1 < len(time_sig_events):
                    duration = time_sig_events[idx + 1][0] - tick
                else:
                    duration = total_ticks - tick
                if duration > best_duration:
                    best_duration = duration
                    best_sig = (num, den)
            if best_sig:
                time_sig_num, time_sig_den = best_sig
                print(f"  Main time signature (longest span): {time_sig_num}/{time_sig_den}")

    bpm = round(mido.tempo2bpm(tempo))
    print(f"  Detected BPM: {bpm}")
    print(f"  Time Signature: {time_sig_num}/{time_sig_den}")

    tpb = mid.ticks_per_beat

    # Determine if this is a type 0 (single track) or type 1 (multi-track) MIDI
    # For type 1, we check if different tracks correspond to different hands

    # Collect channels used per track to understand the structure
    track_channels = {}
    for i, track in enumerate(mid.tracks):
        channels = set()
        for msg in track:
            if hasattr(msg, 'channel'):
                channels.add(msg.channel)
        track_channels[i] = channels
        if channels:
            print(f"  Track {i} channels: {channels}")

    # Determine hand assignment strategy
    # Check if we have a clear 2-track piano arrangement (track for RH, track for LH)
    piano_tracks = []
    for i, track in enumerate(mid.tracks):
        has_notes = any(msg.type == 'note_on' for msg in track)
        if has_notes:
            piano_tracks.append(i)

    print(f"  Tracks with notes: {piano_tracks}")

    # Strategy: if exactly 2 note-bearing tracks, treat first as RH and second as LH
    # If tracks use different channels, use channel-based assignment
    # Otherwise fall back to pitch-based split
    use_track_split = False
    rh_track = None
    lh_track = None

    if len(piano_tracks) == 2:
        # Check track names for hints
        for i in piano_tracks:
            name = mid.tracks[i].name.lower()
            if 'right' in name or 'treble' in name or 'melody' in name:
                rh_track = i
            elif 'left' in name or 'bass' in name:
                lh_track = i

        if rh_track is None and lh_track is None:
            # Assume first note track = RH, second = LH (common convention)
            rh_track = piano_tracks[0]
            lh_track = piano_tracks[1]
            use_track_split = True
            print(f"  Using track split: RH=Track {rh_track}, LH=Track {lh_track}")
        elif rh_track is not None and lh_track is not None:
            use_track_split = True
            print(f"  Using named track split: RH=Track {rh_track}, LH=Track {lh_track}")

    # Parse note events from all tracks
    all_notes = []

    for track_idx, track in enumerate(mid.tracks):
        abs_tick = 0
        active_notes = {}  # (channel, note) -> (start_tick, velocity)

        for msg in track:
            abs_tick += msg.time

            if msg.type == 'note_on' and msg.velocity > 0:
                key = (msg.channel, msg.note)
                active_notes[key] = (abs_tick, msg.velocity)

            elif msg.type == 'note_off' or (msg.type == 'note_on' and msg.velocity == 0):
                key = (msg.channel, msg.note)
                if key in active_notes:
                    start_tick, velocity = active_notes.pop(key)
                    duration_ticks = abs_tick - start_tick

                    # Convert ticks to beats
                    beat = start_tick / tpb
                    duration = duration_ticks / tpb

                    # Skip very short or zero-duration notes
                    if duration < 0.01:
                        continue

                    # Determine hand
                    if use_track_split:
                        if track_idx == rh_track:
                            hand = "R"
                        elif track_idx == lh_track:
                            hand = "L"
                        else:
                            hand = determine_hand(msg.note, msg.channel)
                    else:
                        # Try channel-based first for multi-channel single track
                        hand = determine_hand(msg.note, msg.channel)

                    pitch_name = midi_note_to_name(msg.note)

                    all_notes.append({
                        "pitch": pitch_name,
                        "beat": round(beat, 4),
                        "duration": round(duration, 4),
                        "hand": hand,
                        "_midi_note": msg.note,  # for sorting, will be removed
                        "_velocity": velocity
                    })

    # Sort notes by beat position, then by pitch (descending for readability)
    all_notes.sort(key=lambda n: (n["beat"], -n["_midi_note"]))

    # Remove internal fields
    for note in all_notes:
        del note["_midi_note"]
        del note["_velocity"]

    print(f"  Total notes extracted: {len(all_notes)}")

    # Round beat values nicely
    for note in all_notes:
        note["beat"] = round(note["beat"], 2)
        note["duration"] = round(note["duration"], 2)

    return {
        "bpm": bpm,
        "timeSignature": f"{time_sig_num}/{time_sig_den}",
        "notes": all_notes
    }


def process_song(filepath, title, artist, difficulty, level):
    """Process a single MIDI file into the full song JSON format."""
    result = parse_midi_file(filepath)

    return {
        "title": title,
        "artist": artist,
        "difficulty": difficulty,
        "bpm": result["bpm"],
        "timeSignature": result["timeSignature"],
        "level": level,
        "notes": result["notes"]
    }


def main():
    output_dir = r"T:\CLAUDE\SYNTROPIANO\app\src\main\assets\songs"

    songs = [
        {
            "filepath": r"C:\Users\timo\Downloads\viva-la-vida-piano-super-easy.mid",
            "title": "Viva La Vida",
            "artist": "Coldplay",
            "difficulty": 2,
            "level": 2,
            "output": "viva-la-vida.json"
        },
        {
            "filepath": r"C:\Users\timo\Downloads\we-wish-you-a-merry-christmas.mid",
            "title": "We Wish You a Merry Christmas",
            "artist": "Traditional",
            "difficulty": 1,
            "level": 1,
            "output": "we-wish-you-a-merry-christmas.json"
        },
        {
            "filepath": r"C:\Users\timo\Downloads\amazing-grace-easy-piano.mid",
            "title": "Amazing Grace",
            "artist": "Traditional",
            "difficulty": 1,
            "level": 1,
            "output": "amazing-grace.json"
        },
        {
            "filepath": r"C:\Users\timo\Downloads\clocks-coldplay.mid",
            "title": "Clocks",
            "artist": "Coldplay",
            "difficulty": 3,
            "level": 2,
            "output": "clocks.json"
        },
        {
            "filepath": r"C:\Users\timo\Downloads\when-i-was-your-man-bruno-mars.mid",
            "title": "When I Was Your Man",
            "artist": "Bruno Mars",
            "difficulty": 2,
            "level": 2,
            "output": "when-i-was-your-man.json"
        },
        {
            "filepath": r"C:\Users\timo\Downloads\jingle-bells.mid",
            "title": "Jingle Bells",
            "artist": "Traditional",
            "difficulty": 1,
            "level": 1,
            "output": "jingle-bells.json"
        },
        {
            "filepath": r"C:\Users\timo\Downloads\still-dre-variation-composition.mid",
            "title": "Still D.R.E. (Variation)",
            "artist": "Dr. Dre ft. Snoop Dogg",
            "difficulty": 3,
            "level": 3,
            "output": "still-dre-variation.json"
        },
    ]

    for song in songs:
        print(f"\n{'='*60}")
        print(f"Processing: {song['title']} by {song['artist']}")
        print(f"{'='*60}")

        try:
            result = process_song(
                song["filepath"],
                song["title"],
                song["artist"],
                song["difficulty"],
                song["level"]
            )

            output_path = os.path.join(output_dir, song["output"])
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(result, f, indent=2, ensure_ascii=False)

            print(f"\n  OUTPUT: {output_path}")
            print(f"  Notes: {len(result['notes'])}")
            print(f"  BPM: {result['bpm']}")
            print(f"  Time Sig: {result['timeSignature']}")

            # Show first 5 notes as preview
            print(f"\n  First 5 notes:")
            for note in result['notes'][:5]:
                print(f"    {note}")

        except Exception as e:
            print(f"  ERROR processing {song['filepath']}: {e}")
            import traceback
            traceback.print_exc()

    print(f"\n{'='*60}")
    print("All songs processed!")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
