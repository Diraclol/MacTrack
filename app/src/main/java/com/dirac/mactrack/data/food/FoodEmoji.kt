package com.dirac.mactrack.data.food

// Best-effort emoji for a food, chosen by keyword-matching its name. Display-only: no storage,
// no network, works for every source (custom, CNF, branded, meal entries). First matching
// keyword wins, so more specific terms must come before shorter ones they contain
// (e.g. "peanut" before "pea", "watermelon" before "melon", "hamburger" before "ham").
private val EMOJI_KEYWORDS: List<Pair<String, String>> = listOf(
    // multi-word / specific first
    "ice cream" to "🍦", "peanut butter" to "🥜", "peanut" to "🥜",
    "protein shake" to "🥤", "protein" to "🥤", "french fries" to "🍟",
    "fries" to "🍟", "hamburger" to "🍔", "burger" to "🍔",
    "hot dog" to "🌭", "pizza" to "🍕", "sandwich" to "🥪",
    "taco" to "🌮", "burrito" to "🌯", "wrap" to "🌯",
    "sushi" to "🍣", "ramen" to "🍜", "noodle" to "🍜",
    "spaghetti" to "🍝", "pasta" to "🍝", "curry" to "🍛",
    "rice" to "🍚", "bagel" to "🥯", "pretzel" to "🥨",
    "pancake" to "🥞", "waffle" to "🧇", "toast" to "🍞",
    "bread" to "🍞", "cereal" to "🥣", "oat" to "🥣",
    "cracker" to "🍘", "popcorn" to "🍿", "chip" to "🍟",
    "soup" to "🍲", "stew" to "🍲", "salad" to "🥗",
    // proteins
    "chicken" to "🍗", "turkey" to "🦃", "steak" to "🥩",
    "beef" to "🥩", "bacon" to "🥓", "pork" to "🥓",
    "ham" to "🍖", "shrimp" to "🍤", "salmon" to "🐟",
    "tuna" to "🐟", "fish" to "🐟", "egg" to "🥚",
    // dairy
    "milk" to "🥛", "yogurt" to "🥛", "yoghurt" to "🥛",
    "cheese" to "🧀", "butter" to "🧈",
    // sweets
    "chocolate" to "🍫", "cookie" to "🍪", "cake" to "🍰",
    "donut" to "🍩", "doughnut" to "🍩", "pie" to "🥧",
    "candy" to "🍬", "honey" to "🍯", "jam" to "🍯",
    // drinks
    "coffee" to "☕", "tea" to "🍵", "juice" to "🧃",
    "soda" to "🥤", "cola" to "🥤", "smoothie" to "🥤",
    "shake" to "🥤", "beer" to "🍺", "wine" to "🍷",
    "water" to "💧",
    // fruit
    "apple" to "🍎", "banana" to "🍌", "grape" to "🍇",
    "orange" to "🍊", "strawberr" to "🍓", "blueberr" to "🫐",
    "berry" to "🫐", "watermelon" to "🍉", "melon" to "🍈",
    "peach" to "🍑", "pear" to "🍐", "pineapple" to "🍍",
    "mango" to "🥭", "cherry" to "🍒", "lemon" to "🍋",
    "lime" to "🍋", "kiwi" to "🥝", "avocado" to "🥑",
    "coconut" to "🥥",
    // veg
    "tomato" to "🍅", "carrot" to "🥕", "broccoli" to "🥦",
    "corn" to "🌽", "potato" to "🥔", "pepper" to "🌶️",
    "cucumber" to "🥒", "lettuce" to "🥬", "mushroom" to "🍄",
    "onion" to "🧅", "garlic" to "🧄", "eggplant" to "🍆",
    "pea" to "🫘", "bean" to "🫘",
    // nuts & misc
    "almond" to "🥜", "walnut" to "🥜", "nut" to "🥜",
    "seed" to "🌰", "salt" to "🧂", "oil" to "🫒"
)

// Generic "plate" fallback when nothing matches.
private const val DEFAULT_EMOJI = "🍽️"

fun foodEmoji(name: String): String {
    val n = name.lowercase()
    for ((keyword, emoji) in EMOJI_KEYWORDS) {
        if (n.contains(keyword)) return emoji
    }
    return DEFAULT_EMOJI
}

// The icon to show for a saved food: the user's chosen emoji if set, otherwise the
// name-derived one. Used everywhere a FoodItem's icon is rendered.
fun foodIcon(emoji: String?, name: String): String =
    emoji?.takeIf { it.isNotBlank() } ?: foodEmoji(name)
