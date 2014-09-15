package com.aegiswallet.utils;


public class SMSTools {
    private static String number;
    private static String message;
    private static boolean numbertype;       // national/international dialing number

    public SMSTools(String no, boolean notype, String msg) {
        number = no;
        message = msg;
        numbertype = notype;
    }

    /**
     * Convert a dialing number into the GSM format
     * @param number dialing number
     * @return coded dialing number
     */
    private static byte[] convertDialNumber(String number) {
        int l = number.length();
        int j = 0;  // index in addr
        int n;      // length of converted dial number
        byte[] data;

        // calculate length of converted dialing number
        n = l / 2;
        if (l % 2 != 0) {
            n++;
        }
        data = new byte[n];
        for (int i = 0; i < n; i++) {
            switch (number.charAt(j)) {
                case '0': data[i] += 0x00; break;
                case '1': data[i] += 0x01; break;
                case '2': data[i] += 0x02; break;
                case '3': data[i] += 0x03; break;
                case '4': data[i] += 0x04; break;
                case '5': data[i] += 0x05; break;
                case '6': data[i] += 0x06; break;
                case '7': data[i] += 0x07; break;
                case '8': data[i] += 0x08; break;
                case '9': data[i] += 0x09; break;
            } // switch
            if (j + 1 < l) {
                switch (number.charAt(j + 1)) {
                    case '0': data[i] += 0x00; break;
                    case '1': data[i] += 0x10; break;
                    case '2': data[i] += 0x20; break;
                    case '3': data[i] += 0x30; break;
                    case '4': data[i] += 0x40; break;
                    case '5': data[i] += 0x50; break;
                    case '6': data[i] += 0x60; break;
                    case '7': data[i] += 0x70; break;
                    case '8': data[i] += 0x80; break;
                    case '9': data[i] += 0x90; break;
                } // switch
            } // if
            else {
                data[i] += 0xF0;
            } // else
            j += 2;
        }  // for
        return data;
    } // convertDialNumber

    /**
     * Convert a Unicode text string into the GSM standard alphabet
     * @param msg text string in ASCII
     * @return text string in GSM standard alphabet
     */
    public static byte[] convertUnicode2GSM(String msg) {
        byte[] data = new byte[msg.length()];

        for (int i = 0; i < msg.length(); i++) {
            switch (msg.charAt(i)) {
                case '@':  data[i] = 0x00; break;
                case '$':  data[i] = 0x02; break;
                case '\n': data[i] = 0x0A; break;
                case '\r': data[i] = 0x0D; break;
                case '_':  data[i] = 0x11; break;
                case 'ß':  data[i] = 0x1E; break;
                case ' ':  data[i] = 0x20; break;
                case '!':  data[i] = 0x21; break;
                case '\"': data[i] = 0x22; break;
                case '#':  data[i] = 0x23; break;
                case '%':  data[i] = 0x25; break;
                case '&':  data[i] = 0x26; break;
                case '\'': data[i] = 0x27; break;
                case '(':  data[i] = 0x28; break;
                case ')':  data[i] = 0x29; break;
                case '*':  data[i] = 0x2A; break;
                case '+':  data[i] = 0x2B; break;
                case ',':  data[i] = 0x2C; break;
                case '-':  data[i] = 0x2D; break;
                case '.':  data[i] = 0x2E; break;
                case '/':  data[i] = 0x2F; break;
                case '0':  data[i] = 0x30; break;
                case '1':  data[i] = 0x31; break;
                case '2':  data[i] = 0x32; break;
                case '3':  data[i] = 0x33; break;
                case '4':  data[i] = 0x34; break;
                case '5':  data[i] = 0x35; break;
                case '6':  data[i] = 0x36; break;
                case '7':  data[i] = 0x37; break;
                case '8':  data[i] = 0x38; break;
                case '9':  data[i] = 0x39; break;
                case ':':  data[i] = 0x3A; break;
                case ';':  data[i] = 0x3B; break;
                case '<':  data[i] = 0x3C; break;
                case '=':  data[i] = 0x3D; break;
                case '>':  data[i] = 0x3E; break;
                case '?':  data[i] = 0x3F; break;
                case 'A':  data[i] = 0x41; break;
                case 'B':  data[i] = 0x42; break;
                case 'C':  data[i] = 0x43; break;
                case 'D':  data[i] = 0x44; break;
                case 'E':  data[i] = 0x45; break;
                case 'F':  data[i] = 0x46; break;
                case 'G':  data[i] = 0x47; break;
                case 'H':  data[i] = 0x48; break;
                case 'I':  data[i] = 0x49; break;
                case 'J':  data[i] = 0x4A; break;
                case 'K':  data[i] = 0x4B; break;
                case 'L':  data[i] = 0x4C; break;
                case 'M':  data[i] = 0x4D; break;
                case 'N':  data[i] = 0x4E; break;
                case 'O':  data[i] = 0x4F; break;
                case 'P':  data[i] = 0x50; break;
                case 'Q':  data[i] = 0x51; break;
                case 'R':  data[i] = 0x52; break;
                case 'S':  data[i] = 0x53; break;
                case 'T':  data[i] = 0x54; break;
                case 'U':  data[i] = 0x55; break;
                case 'V':  data[i] = 0x56; break;
                case 'W':  data[i] = 0x57; break;
                case 'X':  data[i] = 0x58; break;
                case 'Y':  data[i] = 0x59; break;
                case 'Z':  data[i] = 0x5A; break;
                case 'Ä':  data[i] = 0x5B; break;
                case 'Ö':  data[i] = 0x5C; break;
                case 'Ü':  data[i] = 0x5E; break;
                case '§':  data[i] = 0x5F; break;
                case 'a':  data[i] = 0x61; break;
                case 'b':  data[i] = 0x62; break;
                case 'c':  data[i] = 0x63; break;
                case 'd':  data[i] = 0x64; break;
                case 'e':  data[i] = 0x65; break;
                case 'f':  data[i] = 0x66; break;
                case 'g':  data[i] = 0x67; break;
                case 'h':  data[i] = 0x68; break;
                case 'i':  data[i] = 0x69; break;
                case 'j':  data[i] = 0x6A; break;
                case 'k':  data[i] = 0x6B; break;
                case 'l':  data[i] = 0x6C; break;
                case 'm':  data[i] = 0x6D; break;
                case 'n':  data[i] = 0x6E; break;
                case 'o':  data[i] = 0x6F; break;
                case 'p':  data[i] = 0x70; break;
                case 'q':  data[i] = 0x71; break;
                case 'r':  data[i] = 0x72; break;
                case 's':  data[i] = 0x73; break;
                case 't':  data[i] = 0x74; break;
                case 'u':  data[i] = 0x75; break;
                case 'v':  data[i] = 0x76; break;
                case 'w':  data[i] = 0x77; break;
                case 'x':  data[i] = 0x78; break;
                case 'y':  data[i] = 0x79; break;
                case 'z':  data[i] = 0x7A; break;
                case 'ä':  data[i] = 0x7B; break;
                case 'ö':  data[i] = 0x7C; break;
                case 'ü':  data[i] = 0x7E; break;
                default:   data[i] = 0x3F; break; // '?'
            }  // switch
        }  // for
        return data;
    }  // convertUnicode2GSM

    /**
     * Convert one GSM standard alphabet character into a Unicode character
     * @param b one GSM standard alphabet character
     * @return one Unicode character
     */
    public static char convertGSM2Unicode(int b) {
        char c;

        if ((b >= 0x41) && (b <= 0x5A)) {    // character is between "A" and "Z"
            c = (char) b;
            return c;
        }  // if
        if ((b >= 0x61) && (b <= 0x7A)) {    // character is between "a" and "z"
            c = (char) b;
            return c;
        }  // if
        if ((b >= 0x30) && (b <= 0x39)) {    // character is between "0" and "9"
            c = (char) b;
            return c;
        }  // if

        switch (b) {
            case 0x00 : c = '@'; break;
            case 0x02 : c = '$'; break;
            case 0x0A : c = '\n'; break;
            case 0x0D : c = '\r'; break;
            case 0x11 : c = '_'; break;
            case 0x1E : c = 'ß'; break;
            case 0x20 : c = ' '; break;
            case 0x21 : c = '!'; break;
            case 0x22 : c = '\"'; break;
            case 0x23 : c = '#'; break;
            case 0x25 : c = '%'; break;
            case 0x26 : c = '&'; break;
            case 0x27 : c = '\''; break;
            case 0x28 : c = '('; break;
            case 0x29 : c = ')'; break;
            case 0x2A : c = '*'; break;
            case 0x2B : c = '+'; break;
            case 0x2C : c = ','; break;
            case 0x2D : c = '-'; break;
            case 0x2E : c = '.'; break;
            case 0x2F : c = '/'; break;
            case 0x3A : c = ':'; break;
            case 0x3B : c = ';'; break;
            case 0x3C : c = '<'; break;
            case 0x3D : c = '='; break;
            case 0x3E : c = '>'; break;
            case 0x3F : c = '?'; break;
            case 0x5B : c = 'Ä'; break;
            case 0x5C : c = 'Ö'; break;
            case 0x5E : c = 'Ü'; break;
            case 0x5F : c = '§'; break;
            case 0x7B : c = 'ä'; break;
            case 0x7C : c = 'ö'; break;
            case 0x7E : c = 'ü'; break;
            default:    c = '?'; break;
        }  // switch

        return c;
    }  // convertGSM2Unicode

    /**
     * Compress a readable text message into the GSM standard alphabet
     * (1 character -> 7 bit data)
     * @param data text string in Unicode
     * @return text string in GSM standard alphabet
     */
    public static byte[] compress(byte[] data) {
        int l;
        int n;  // length of compressed data
        byte[] comp;

        // calculate length of message
        l = data.length;
        n = (l * 7) / 8;
        if ((l * 7) % 8 != 0) {
            n++;
        }  // if

        comp = new byte[n];
        int j = 0;   // index in data
        int s = 0;   // shift from next data byte
        for (int i = 0; i < n; i++) {
            comp[i] = (byte)((data[j] & 0x7F) >>> s);
            s++;
            if (j + 1 < l) {
                comp[i] += (byte)((data[j + 1] << (8 - s)) & 0xFF);
            }  // if
            if (s < 7) {
                j++;
            }  // if
            else  {
                s = 0;
                j += 2;
            }  // else
        } // for
        return comp;
    }  // compress

    /**
     * Extracts from a given SMS the Text
     * @param data ms SMS string
     * @return text date, time and SMS text string in Unicode
     */
    public static String getSMSText(String data) {
        int i, x, n;
        String s, date="", time="";

        // delete LF at the end of the PDU
        i = data.lastIndexOf("\n");
        if (data.length()-1 == i) {
            data = data.substring(0, i-1);
        }  // if

        // delete the AT command information at the beginning of the PDU
        i = data.lastIndexOf("\n");
        data = data.substring(i+1, data.length());

        s = data.substring(0, 2);
        x = Integer.parseInt(s, 10);  // get length [byte] of delivering SMSC number
        i = 2 + x * 2;                // set index to message header flags
        i = i + 2;                    // set index to length [digits] of originating adress
        s = data.substring(i, i+2);    // get length [digits] of originating adress
        x = Integer.parseInt(s, 10);
        i = i + 2 + x * 2;            // set index to data coding scheme
        i = i + 2;                    // set index to date and time

        // get data and time
        date = data.substring(i+1, i+2) + data.substring(i, i+1) ;                // get year
        date = data.substring(i+3, i+4) + data.substring(i+2, i+3) + "." + date;  // get month
        date = data.substring(i+5, i+6) + data.substring(i+4, i+5) + "." + date;  // get day

        time = data.substring(i+11, i+12) + data.substring(i+10, i+11);            // get hour
        time = data.substring(i+9, i+10) + data.substring(i+8, i+9) + ":" + time;  // get minute
        time = data.substring(i+7, i+8) + data.substring(i+6, i+7) + ":" + time;   // get second

        i = i + 14;                              // set index to length of user data (=SMS)
        s = data.substring(i, i+2);               // get length of user data (=SMS)
        x = Integer.parseInt(s, 16);             // calculate length [characters] of user data (=SMS)
        data = data.substring(i+2, data.length());  // delete the transport information at the beginning of the PDU

        // copy SMS from a string into a byte array
        byte sms[] = new byte[data.length()/2];
        for (n = 0; n < data.length()/2; n++) {
            s = data.substring(n*2, n*2+2);
            sms[n] = (byte)(0x000000FF & Integer.parseInt(s, 16));
        }  // for

        data = expand(sms);
        data = date + " " + time + "  " + data;
        return data;
    }  // getSMSText

    /**
     * Expands a compressed GSM message in a readable text message
     * (7 bit data -> 1 character)
     * @param indata text string in GSM standard alphabet
     * @return text string in Unicode
     */
    public static String expand(byte[] indata) {
        int x, n, y, Bytebefore, Bitshift;
        String msg = new String("");
        byte data[] = new byte[indata.length+1];

        for (n = 1; n < data.length; n++) {
            data[n] = indata[n-1];
        }

        Bytebefore = 0;
        for (n = 1; n < data.length; n++) {
            x = (int) (0x000000FF & data[n]);   // get a byte from the SMS
            Bitshift = (n-1) % 7;               // calculate number of neccssary bit shifts
            y = x;
            y = y << Bitshift;                  // shift to get a conversion 7 bit compact GSM -> Unicode
            y = y | Bytebefore;                 // add bits from the byte before this byte
            y = y & 0x0000007F;                 // delete all bits except bit 7 ... 1 of the byte
            msg = msg + convertGSM2Unicode(y);  // conversion: 7 bit GSM character -> Unicode
            if (Bitshift == 6) {
                Bitshift = 1;
                y = x;
                y = y >>> Bitshift;                 // shift to get a conversion 7 bit compact GSM -> Unicode
                y = y & 0x0000007F;                 // delete all bits except bit 7 ... 1 of the byte
                msg = msg + convertGSM2Unicode(y);  // conversion: 7 bit GSM character -> Unicode
                Bytebefore = 0;
            }  // if
            else {
                Bytebefore = x;
                Bitshift = 7 - Bitshift;
                Bytebefore = Bytebefore >>> Bitshift;  // shift to get a conversion 7 bit compact GSM -> Unicode
                Bytebefore = Bytebefore & 0x000000FF;  // mask for one byte
            }  // else
        }  // for
        return msg;
    }  // expand

    /**
     * Convert data into a hex string
     * @param data to convert
     * @return in hex string converted data
     */
    public static char[] toHexString(byte[] data) {
        int l = data.length;
        char[] hex = new char[2 * l];

        int j = 0; // index in hex
        for (int i = 0; i < data.length; i++) {
            switch (data[i] & 0xF0) {
                case 0x00: hex[j] = '0'; break;
                case 0x10: hex[j] = '1'; break;
                case 0x20: hex[j] = '2'; break;
                case 0x30: hex[j] = '3'; break;
                case 0x40: hex[j] = '4'; break;
                case 0x50: hex[j] = '5'; break;
                case 0x60: hex[j] = '6'; break;
                case 0x70: hex[j] = '7'; break;
                case 0x80: hex[j] = '8'; break;
                case 0x90: hex[j] = '9'; break;
                case 0xA0: hex[j] = 'A'; break;
                case 0xB0: hex[j] = 'B'; break;
                case 0xC0: hex[j] = 'C'; break;
                case 0xD0: hex[j] = 'D'; break;
                case 0xE0: hex[j] = 'E'; break;
                case 0xF0: hex[j] = 'F'; break;
            } // switch
            j++;
            switch (data[i] & 0x0F) {
                case 0x00: hex[j] = '0'; break;
                case 0x01: hex[j] = '1'; break;
                case 0x02: hex[j] = '2'; break;
                case 0x03: hex[j] = '3'; break;
                case 0x04: hex[j] = '4'; break;
                case 0x05: hex[j] = '5'; break;
                case 0x06: hex[j] = '6'; break;
                case 0x07: hex[j] = '7'; break;
                case 0x08: hex[j] = '8'; break;
                case 0x09: hex[j] = '9'; break;
                case 0x0A: hex[j] = 'A'; break;
                case 0x0B: hex[j] = 'B'; break;
                case 0x0C: hex[j] = 'C'; break;
                case 0x0D: hex[j] = 'D'; break;
                case 0x0E: hex[j] = 'E'; break;
                case 0x0F: hex[j] = 'F'; break;
            }  // switch
            j++;
        }  // for
        return hex;
    }  // toHexString

}   // SMSTools