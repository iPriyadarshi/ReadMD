package com.readmd.app.domain

/**
 * Replaces GitHub-style emoji shortcodes such as `:smile:` with the emoji itself. Unknown
 * codes are left as they are, and fenced code, inline code and escaped colons are never
 * touched.
 */
object EmojiShortcodes {

    private val OPENING_FENCE = Regex("^\\s*(`{3,}|~{3,})")

    fun lookup(name: String): String? = EMOJI[name.lowercase()]

    fun prepare(markdown: String): String {
        if (':' !in markdown) return markdown

        val out = ArrayList<String>()
        var fenceChar = ' '
        var fenceLength = 0
        for (line in markdown.split("\n")) {
            val bare = line.trimEnd('\r')
            if (fenceLength > 0) {
                if (bare.trim().let { it.length >= fenceLength && it.all { c -> c == fenceChar } }) fenceLength = 0
                out += line
                continue
            }
            val fence = OPENING_FENCE.find(bare)?.groupValues?.get(1)
            if (fence != null) {
                fenceChar = fence[0]
                fenceLength = fence.length
                out += line
                continue
            }
            out += convertLine(line)
        }
        return out.joinToString("\n")
    }

    private fun convertLine(line: String): String {
        if (':' !in line) return line
        val sb = StringBuilder(line.length)
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '`' -> {
                    var n = 0
                    while (i + n < line.length && line[i + n] == '`') n++
                    val close = line.indexOf("`".repeat(n), i + n)
                    val end = if (close >= 0) close + n else i + n
                    sb.append(line, i, end)
                    i = end
                }
                c == '\\' && i + 1 < line.length -> {
                    sb.append(c).append(line[i + 1])
                    i += 2
                }
                c == ':' -> {
                    var j = i + 1
                    while (j < line.length && isNameChar(line[j])) j++
                    val emoji = if (j > i + 1 && j < line.length && line[j] == ':') {
                        lookup(line.substring(i + 1, j))
                    } else {
                        null
                    }
                    if (emoji != null) {
                        sb.append(emoji)
                        i = j + 1
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun isNameChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '+' || c == '-'

    private val EMOJI: Map<String, String> = mapOf(
        // Faces
        "smile" to "😄", "smiley" to "😃", "grinning" to "😀", "laughing" to "😆", "satisfied" to "😆",
        "joy" to "😂", "rofl" to "🤣", "wink" to "😉", "blush" to "😊", "innocent" to "😇",
        "slightly_smiling_face" to "🙂", "upside_down_face" to "🙃", "heart_eyes" to "😍",
        "kissing_heart" to "😘", "sunglasses" to "😎", "nerd_face" to "🤓", "thinking" to "🤔",
        "neutral_face" to "😐", "expressionless" to "😑", "unamused" to "😒", "roll_eyes" to "🙄",
        "grimacing" to "😬", "relieved" to "😌", "pensive" to "😔", "sleepy" to "😪", "sleeping" to "😴",
        "mask" to "😷", "stuck_out_tongue" to "😛", "stuck_out_tongue_winking_eye" to "😜", "smirk" to "😏",
        "disappointed" to "😞", "worried" to "😟", "angry" to "😠", "rage" to "😡", "cry" to "😢",
        "sob" to "😭", "scream" to "😱", "fearful" to "😨", "cold_sweat" to "😰", "sweat_smile" to "😅",
        "hugs" to "🤗", "zipper_mouth_face" to "🤐", "confused" to "😕", "open_mouth" to "😮",
        "astonished" to "😲", "flushed" to "😳", "dizzy_face" to "😵", "skull" to "💀", "ghost" to "👻",
        "alien" to "👽", "robot" to "🤖", "poop" to "💩", "hankey" to "💩", "shit" to "💩",
        "clown_face" to "🤡", "see_no_evil" to "🙈", "hear_no_evil" to "🙉", "speak_no_evil" to "🙊",
        // Hands and people
        "+1" to "👍", "thumbsup" to "👍", "-1" to "👎", "thumbsdown" to "👎", "wave" to "👋",
        "raised_hands" to "🙌", "clap" to "👏", "pray" to "🙏", "ok_hand" to "👌", "v" to "✌️",
        "muscle" to "💪", "point_up" to "☝️", "point_down" to "👇", "point_left" to "👈",
        "point_right" to "👉", "fist" to "✊", "punch" to "👊", "handshake" to "🤝", "eyes" to "👀",
        "brain" to "🧠", "crown" to "👑", "ring" to "💍",
        // Hearts and symbols
        "heart" to "❤️", "broken_heart" to "💔", "blue_heart" to "💙", "green_heart" to "💚",
        "yellow_heart" to "💛", "purple_heart" to "💜", "black_heart" to "🖤", "sparkling_heart" to "💖",
        "two_hearts" to "💕", "fire" to "🔥", "sparkles" to "✨", "star" to "⭐", "star2" to "🌟",
        "zap" to "⚡", "boom" to "💥", "collision" to "💥", "tada" to "🎉", "confetti_ball" to "🎊",
        "balloon" to "🎈", "gift" to "🎁", "trophy" to "🏆", "100" to "💯", "warning" to "⚠️",
        "x" to "❌", "heavy_check_mark" to "✔️", "white_check_mark" to "✅", "heavy_plus_sign" to "➕",
        "heavy_minus_sign" to "➖", "question" to "❓", "exclamation" to "❗", "heavy_exclamation_mark" to "❗",
        "bangbang" to "‼️", "no_entry" to "⛔", "stop_sign" to "🛑", "construction" to "🚧",
        "rotating_light" to "🚨", "information_source" to "ℹ️", "copyright" to "©️", "registered" to "®️",
        "tm" to "™️", "new" to "🆕", "free" to "🆓", "up" to "🆙", "ok" to "🆗", "cool" to "🆒", "sos" to "🆘",
        // Arrows
        "arrow_right" to "➡️", "arrow_left" to "⬅️", "arrow_up" to "⬆️", "arrow_down" to "⬇️",
        "arrow_forward" to "▶️", "rewind" to "⏪", "fast_forward" to "⏩", "repeat" to "🔁",
        "recycle" to "♻️", "back" to "🔙", "soon" to "🔜",
        // Objects and tools
        "lock" to "🔒", "unlock" to "🔓", "key" to "🔑", "mag" to "🔍", "bulb" to "💡", "memo" to "📝",
        "pencil" to "📝", "book" to "📖", "books" to "📚", "bookmark" to "🔖", "link" to "🔗",
        "paperclip" to "📎", "pushpin" to "📌", "calendar" to "📅", "hourglass" to "⌛", "alarm_clock" to "⏰",
        "bell" to "🔔", "mega" to "📣", "email" to "📧", "envelope" to "✉️", "inbox_tray" to "📥",
        "outbox_tray" to "📤", "package" to "📦", "computer" to "💻", "desktop_computer" to "🖥️",
        "keyboard" to "⌨️", "iphone" to "📱", "telephone_receiver" to "📞", "battery" to "🔋",
        "electric_plug" to "🔌", "wrench" to "🔧", "hammer" to "🔨", "gear" to "⚙️", "nut_and_bolt" to "🔩",
        "hammer_and_wrench" to "🛠️", "toolbox" to "🧰", "microscope" to "🔬", "telescope" to "🔭",
        "test_tube" to "🧪", "pill" to "💊", "syringe" to "💉", "moneybag" to "💰", "dollar" to "💵",
        "credit_card" to "💳", "gem" to "💎", "chart_with_upwards_trend" to "📈",
        "chart_with_downwards_trend" to "📉", "bar_chart" to "📊", "clipboard" to "📋",
        "file_folder" to "📁", "open_file_folder" to "📂", "page_facing_up" to "📄", "scroll" to "📜",
        "newspaper" to "📰", "label" to "🏷️", "checkered_flag" to "🏁", "triangular_flag_on_post" to "🚩",
        "mortar_board" to "🎓", "eyeglasses" to "👓", "shirt" to "👕", "jeans" to "👖", "tophat" to "🎩",
        "lipstick" to "💄", "dart" to "🎯", "art" to "🎨", "video_game" to "🎮", "musical_note" to "🎵",
        "notes" to "🎶", "microphone" to "🎤", "headphones" to "🎧", "guitar" to "🎸", "eight_ball" to "🎱",
        // Travel and weather
        "rocket" to "🚀", "airplane" to "✈️", "car" to "🚗", "bike" to "🚲", "house" to "🏠",
        "earth_africa" to "🌍", "globe_with_meridians" to "🌐", "sunny" to "☀️", "cloud" to "☁️",
        "umbrella" to "☔", "snowflake" to "❄️", "rainbow" to "🌈", "ocean" to "🌊",
        // Nature
        "evergreen_tree" to "🌲", "seedling" to "🌱", "herb" to "🌿", "four_leaf_clover" to "🍀",
        "cactus" to "🌵", "sunflower" to "🌻", "rose" to "🌹", "tulip" to "🌷",
        // Food and drink
        "apple" to "🍎", "banana" to "🍌", "pizza" to "🍕", "hamburger" to "🍔", "fries" to "🍟",
        "coffee" to "☕", "tea" to "🍵", "beer" to "🍺", "beers" to "🍻", "wine_glass" to "🍷",
        "cake" to "🍰", "birthday" to "🎂", "cookie" to "🍪",
        // Animals
        "dog" to "🐶", "cat" to "🐱", "mouse" to "🐭", "rabbit" to "🐰", "bear" to "🐻", "panda_face" to "🐼",
        "tiger" to "🐯", "lion" to "🦁", "cow" to "🐮", "pig" to "🐷", "monkey_face" to "🐵", "chicken" to "🐔",
        "penguin" to "🐧", "bird" to "🐦", "turtle" to "🐢", "snake" to "🐍", "unicorn" to "🦄",
        "bee" to "🐝", "honeybee" to "🐝", "butterfly" to "🦋", "whale" to "🐳", "fish" to "🐟",
        "shark" to "🦈", "octopus" to "🐙", "crab" to "🦀", "snail" to "🐌", "bug" to "🐛", "ant" to "🐜",
    )
}
