import javax.usb.*

fun findDevice(hub: UsbHub, idVendor: Short, idProduct: Short): UsbDevice? {
    val devices = hub.attachedUsbDevices as List<UsbDevice>
    for (d in devices) {
        val descriptor = d.usbDeviceDescriptor
        if (idVendor == descriptor.idVendor() && idProduct == descriptor.idProduct()) {
            return d
        }

        if (d.isUsbHub) {
            val newD = findDevice(d as UsbHub, idVendor, idProduct)
            if (newD != null) { return newD }
        }
    }

    return null
}

val convTable = mapOf(0 to arrayOf("", ""),
    4 to arrayOf("a", "A"),
    5 to arrayOf("b", "B"),
    6 to arrayOf("c", "C"),
    7 to arrayOf("d", "D"),
    8 to arrayOf("e", "E"),
    9 to arrayOf("f", "F"),
    10 to arrayOf("g", "G"),
    11 to arrayOf("h", "H"),
    12 to arrayOf("i", "I"),
    13 to arrayOf("j", "J"),
    14 to arrayOf("k", "K"),
    15 to arrayOf("l", "L"),
    16 to arrayOf("m", "M"),
    17 to arrayOf("n", "N"),
    18 to arrayOf("o", "O"),
    19 to arrayOf("p", "P"),
    20 to arrayOf("q", "Q"),
    21 to arrayOf("r", "R"),
    22 to arrayOf("s", "S"),
    23 to arrayOf("t", "T"),
    24 to arrayOf("u", "U"),
    25 to arrayOf("v", "V"),
    26 to arrayOf("w", "W"),
    27 to arrayOf("x", "X"),
    28 to arrayOf("y", "Y"),
    29 to arrayOf("z", "Z"),
    30 to arrayOf("1", "!"),
    31 to arrayOf("2", "@"),
    32 to arrayOf("3", "#"),
    33 to arrayOf("4", "$"),
    34 to arrayOf("5", "%"),
    35 to arrayOf("6", "^"),
    36 to arrayOf("7", "&"),
    37 to arrayOf("8", "*"),
    38 to arrayOf("9", "("),
    39 to arrayOf("0", ")"),
    40 to arrayOf("\n", "\n"),
    41 to arrayOf("\uE01B", "\uE01B"), // символ escape
    42 to arrayOf("\b", "\b"),
    43 to arrayOf("\t", "\t"),
    44 to arrayOf(" ", " "),
    45 to arrayOf("_", "_"),
    46 to arrayOf("=", "+"),
    47 to arrayOf("[", "{"),
    48 to arrayOf("]", "}"),
    49 to arrayOf("\\", "|"),
    50 to arrayOf("#", "~"),
    51 to arrayOf(";", ":"),
    52 to arrayOf("'", "\""),
    53 to arrayOf("`", "~"),
    54 to arrayOf(",", "<"),
    55 to arrayOf(".", ">"),
    56 to arrayOf("/", "?"),
    100 to arrayOf("\\", "|"),
    103 to arrayOf("=", "="))

fun hidToString(octet: ByteArray): String {
    // The USB HID device sends an 8-byte code for every character. This
    // routine converts the HID code to an ASCII character.

    // See https://www.usb.org/sites/default/files/documents/hut1_12v2.pdf
    // for a complete code table. Only relevant codes are used here.

    // Example input from scanner representing the string "http:":
    // array('B', [0, 0, 11, 0, 0, 0, 0, 0])   # h
    // array('B', [0, 0, 23, 0, 0, 0, 0, 0])   # t
    // array('B', [0, 0, 0, 0, 0, 0, 0, 0])    # nothing, ignore
    // array('B', [0, 0, 23, 0, 0, 0, 0, 0])   # t
    // array('B', [0, 0, 19, 0, 0, 0, 0, 0])   # p
    // array('B', [2, 0, 51, 0, 0, 0, 0, 0])   # :

    //A 2 in first byte seems to indicate to shift the key. For example
    //a code for ';' but with 2 in first byte really means ':'.

    val shift = if (octet[0] == 2.toByte()) 1 else 0
    val result = convTable[octet[2].toInt()]?.get(shift)
    return result ?: ""
}

fun main(args: Array<String>) {
    val barcodeScanner = findDevice(UsbHostManager.getUsbServices().rootUsbHub, 0x1388, 0x1388)

    if (barcodeScanner == null) {
        println("Scanner not found")
        return
    }

    val usbInterface: UsbInterface = barcodeScanner.activeUsbConfiguration.getUsbInterface(0)

    // detach device for kernel
    usbInterface.claim { true }

    try {
        val pipe = (usbInterface.usbEndpoints[0] as UsbEndpoint).usbPipe
        pipe.open()
        try {
            val sb = StringBuilder()

            // Read the data until found end of the line (\n).
            // In my case, the scanner input ends with this character.
            while (true) {
                val data = ByteArray(8)
                pipe.syncSubmit(data)

                val s = hidToString(data)
                if (s == "\n") {
                    break
                }
                sb.append(s)
            }

            println(sb.toString())
        }
        finally {
            pipe.close()
        }
    } catch (ex: Exception) {
        println(ex.message)
    }
    finally {
        usbInterface.release()
    }
}