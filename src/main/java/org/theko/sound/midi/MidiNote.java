package org.theko.sound.midi;

import java.util.HashMap;
import java.util.Map;

public enum MidiNote {
    // Октава 0
    A0(21), A0_SHARP(22), B0(23),
    // Октава 1
    C1(24), C1_SHARP(25), D1(26), D1_SHARP(27), E1(28), F1(29), F1_SHARP(30), G1(31), G1_SHARP(32),
    // Октава 2
    A2(33), A2_SHARP(34), B2(35),
    // Октава 3
    C3(36), C3_SHARP(37), D3(38), D3_SHARP(39), E3(40), F3(41), F3_SHARP(42), G3(43), G3_SHARP(44),
    // Октава 4
    A4(45), A4_SHARP(46), B4(47),
    C4(48), C4_SHARP(49), D4(50), D4_SHARP(51), E4(52), F4(53), F4_SHARP(54), G4(55), G4_SHARP(56),
    A5(57), A5_SHARP(58), B5(59),
    // Октава 5
    C5(60), C5_SHARP(61), D5(62), D5_SHARP(63), E5(64), F5(65), F5_SHARP(66), G5(67), G5_SHARP(68),
    // Октава 6
    A6(69), A6_SHARP(70), B6(71),
    C6(72), C6_SHARP(73), D6(74), D6_SHARP(75), E6(76), F6(77), F6_SHARP(78), G6(79), G6_SHARP(80),
    A7(81), A7_SHARP(82), B7(83),
    // Октава 7
    C7(84), C7_SHARP(85), D7(86), D7_SHARP(87), E7(88), F7(89), F7_SHARP(90), G7(91), G7_SHARP(92),
    A8(93), A8_SHARP(94), B8(95),
    // Октава 8
    C8(96), C8_SHARP(97), D8(98), D8_SHARP(99), E8(100), F8(101), F8_SHARP(102), G8(103), G8_SHARP(104),
    A9(105), A9_SHARP(106), B9(107),
    C9(108), C9_SHARP(109), D9(110), D9_SHARP(111), E9(112), F9(113), F9_SHARP(114), G9(115), G9_SHARP(116);

    private final int midiNumber;
    private static final Map<Integer, MidiNote> NUMBER_TO_NOTE = new HashMap<>();

    static {
        for (MidiNote note : values()) {
            NUMBER_TO_NOTE.put(note.midiNumber, note);
        }
    }

    MidiNote(int midiNumber) {
        this.midiNumber = midiNumber;
    }

    public int getMidiNumber() {
        return midiNumber;
    }

    public static MidiNote fromNumber(int midiNumber) {
        return NUMBER_TO_NOTE.get(midiNumber);
    }

    @Override
    public String toString() {
        String name = name().replace("_SHARP", "#");
        return name.substring(0, name.length() - 1) + name.charAt(name.length() - 1);
    }
}
