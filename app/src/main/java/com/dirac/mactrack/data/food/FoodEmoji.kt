package com.dirac.mactrack.data.food

// Best-effort emoji for a food, chosen by keyword-matching its name. Display-only: no storage,
// no network, works for every source (custom, CNF, branded, meal entries). First matching
// keyword wins, so more specific terms MUST come before shorter ones they contain
// (e.g. "grapefruit" before "grape", "pineapple" before "apple", "sweet potato" before "potato",
// "cupcake" before "cake", "cheesecake" before "cheese"). When adding, mind that `contains` also
// catches very short keywords inside longer words, so keep short keywords late and specific.
private val EMOJI_KEYWORDS: List<Pair<String, String>> = listOf(
    // --- overrides: specific terms that would otherwise be caught by a shorter keyword below ---
    "protein bar" to "🍫",            // before "protein" (which is a shake)
    "grapefruit" to "🍊",             // before "grape"
    "pineapple" to "🍍",              // before "apple"
    "sweet potato" to "🍠",           // before "potato"
    "sunflower seed" to "🌰", "pumpkin seed" to "🌰",   // before "seed"
    "cheesecake" to "🍰",             // before "cheese" and "cake"
    "cupcake" to "🧁",                // before "cake"
    "milkshake" to "🥤",              // before "milk"
    "cream cheese" to "🧀", "cottage cheese" to "🧀",   // before "cream"
    "watermelon" to "🍉",             // before "water" (else "watermelon" matches "water" -> 💧)
    // --- multi-word / prepared dishes ---
    "ice cream" to "🍦", "peanut butter" to "🥜", "peanut" to "🥜",
    "protein shake" to "🥤", "protein" to "🥤", "french fries" to "🍟",
    "fries" to "🍟", "hamburger" to "🍔", "burger" to "🍔",
    "hot dog" to "🌭", "corn dog" to "🌭", "pizza" to "🍕",
    "sandwich" to "🥪", "taco" to "🌮", "burrito" to "🌯",
    "quesadilla" to "🫓", "wrap" to "🌯", "sushi" to "🍣",
    "ramen" to "🍜", "pho" to "🍜", "noodle" to "🍜",
    "spaghetti" to "🍝", "lasagna" to "🍝", "lasagne" to "🍝",
    "macaroni" to "🍝", "gnocchi" to "🍝", "ravioli" to "🍝",
    "pasta" to "🍝", "risotto" to "🍚", "paella" to "🍚",
    "curry" to "🍛", "quinoa" to "🍚", "couscous" to "🍚",
    "rice" to "🍚", "dumpling" to "🥟", "gyoza" to "🥟",
    "wonton" to "🥟", "potsticker" to "🥟", "spring roll" to "🥟",
    "bagel" to "🥯", "pretzel" to "🥨", "pancake" to "🥞",
    "waffle" to "🧇", "croissant" to "🥐", "baguette" to "🥖",
    "tortilla" to "🫓", "naan" to "🫓", "pita" to "🫓",
    "flatbread" to "🫓", "toast" to "🍞", "scone" to "🍞",
    "biscuit" to "🍪", "bread" to "🍞", "granola" to "🥣",
    "oatmeal" to "🥣", "porridge" to "🥣", "muesli" to "🥣",
    "cereal" to "🥣", "oat" to "🥣", "grits" to "🥣",
    "cracker" to "🍘", "popcorn" to "🍿", "chip" to "🍟",
    "soup" to "🍲", "stew" to "🍲", "chowder" to "🍲",
    "salad" to "🥗", "coleslaw" to "🥗", "omelet" to "🥚",
    "omelette" to "🥚", "frittata" to "🥚", "quiche" to "🥚",
    // --- proteins ---
    "ground beef" to "🥩", "ground turkey" to "🦃", "ground chicken" to "🍗",
    "ground pork" to "🥓", "chicken breast" to "🍗", "chicken thigh" to "🍗",
    "chicken wing" to "🍗", "drumstick" to "🍗", "nugget" to "🍗",
    "chicken" to "🍗", "turkey" to "🦃", "duck" to "🦆",
    "steak" to "🥩", "brisket" to "🥩",
    "veal" to "🥩", "bison" to "🥩", "jerky" to "🥩",
    "beef" to "🥩", "meatball" to "🍖", "meatloaf" to "🍖",
    "lamb" to "🍖", "venison" to "🍖", "ribs" to "🍖",
    "pepperoni" to "🍖", "salami" to "🍖", "prosciutto" to "🍖",
    "bacon" to "🥓", "sausage" to "🌭", "pork" to "🥓",
    "ham" to "🍖", "shrimp" to "🍤", "crab" to "🦀",
    "lobster" to "🦞", "crawfish" to "🦞", "crayfish" to "🦞",
    "oyster" to "🦪", "clam" to "🦪", "mussel" to "🦪",
    "scallop" to "🦪", "squid" to "🦑", "calamari" to "🦑",
    "octopus" to "🐙", "salmon" to "🐟", "tuna" to "🐟",
    "cod" to "🐟", "tilapia" to "🐟", "halibut" to "🐟",
    "trout" to "🐟", "mackerel" to "🐟", "sardine" to "🐟",
    "anchovy" to "🐟", "catfish" to "🐟", "fish" to "🐟",
    "egg" to "🥚", "tofu" to "🍢", "tempeh" to "🍢",
    // --- legumes / plant protein ---
    "lentil" to "🫘", "chickpea" to "🫘", "hummus" to "🫘",
    "edamame" to "🫘", "green bean" to "🫘", "black bean" to "🫘",
    "kidney bean" to "🫘",
    // --- dairy ---
    "milk" to "🥛", "yogurt" to "🥛", "yoghurt" to "🥛",
    "cream" to "🥛", "cheese" to "🧀", "butter" to "🧈",
    // --- sweets ---
    "chocolate" to "🍫", "brownie" to "🍫", "cookie" to "🍪",
    "cake" to "🍰", "muffin" to "🧁", "donut" to "🍩",
    "doughnut" to "🍩", "pie" to "🥧", "tart" to "🥧",
    "pudding" to "🍮", "custard" to "🍮", "flan" to "🍮",
    "gelato" to "🍨", "sorbet" to "🍨", "sherbet" to "🍨",
    "sundae" to "🍨", "candy" to "🍬", "macaron" to "🍬",
    "honey" to "🍯", "syrup" to "🍯", "maple" to "🍯",
    "jam" to "🍯", "jelly" to "🍯",
    // --- drinks ---
    "coffee" to "☕", "latte" to "☕", "cappuccino" to "☕",
    "espresso" to "☕", "mocha" to "☕", "americano" to "☕",
    "matcha" to "🍵", "kombucha" to "🍵", "tea" to "🍵",
    "juice" to "🧃", "soda" to "🥤", "cola" to "🥤",
    "smoothie" to "🥤", "shake" to "🥤", "lemonade" to "🥤",
    "gatorade" to "🥤", "cocktail" to "🍸", "martini" to "🍸",
    "margarita" to "🍸", "mojito" to "🍸", "whiskey" to "🥃",
    "whisky" to "🥃", "bourbon" to "🥃", "vodka" to "🥃",
    "tequila" to "🥃", "brandy" to "🥃", "liquor" to "🥃",
    "champagne" to "🍾", "prosecco" to "🍾", "cider" to "🍺",
    "beer" to "🍺", "wine" to "🍷", "water" to "💧",
    // --- fruit ---
    "apple" to "🍎", "banana" to "🍌", "grape" to "🍇",
    "raisin" to "🍇", "prune" to "🍇", "orange" to "🍊",
    "tangerine" to "🍊", "mandarin" to "🍊", "clementine" to "🍊",
    "strawberr" to "🍓", "blueberr" to "🫐", "raspberr" to "🫐",
    "blackberr" to "🫐", "cranberr" to "🫐", "berry" to "🫐",
    "cantaloupe" to "🍈", "honeydew" to "🍈", "melon" to "🍈",
    "peach" to "🍑", "apricot" to "🍑",
    "nectarine" to "🍑", "plum" to "🍑", "pear" to "🍐",
    "mango" to "🥭", "papaya" to "🥭", "cherry" to "🍒",
    "lemon" to "🍋", "lime" to "🍋", "kiwi" to "🥝",
    "avocado" to "🥑", "coconut" to "🥥",
    // --- veg ---
    "tomato" to "🍅", "carrot" to "🥕", "broccoli" to "🥦",
    "cauliflower" to "🥦", "corn" to "🌽", "potato" to "🥔",
    "jalapeno" to "🌶️", "habanero" to "🌶️", "pepper" to "🌶️",
    "cucumber" to "🥒", "zucchini" to "🥒", "pickle" to "🥒",
    "lettuce" to "🥬", "spinach" to "🥬", "kale" to "🥬",
    "cabbage" to "🥬", "celery" to "🥬", "asparagus" to "🥬",
    "arugula" to "🥬", "collard" to "🥬", "mushroom" to "🍄",
    "onion" to "🧅", "garlic" to "🧄", "eggplant" to "🍆",
    "pea" to "🫘", "bean" to "🫘", "yam" to "🍠",
    "pumpkin" to "🎃",
    // --- nuts & misc ---
    "almond" to "🥜", "walnut" to "🥜", "cashew" to "🥜",
    "pistachio" to "🥜", "pecan" to "🥜", "hazelnut" to "🥜",
    "macadamia" to "🥜", "nut" to "🥜", "seed" to "🌰",
    "salsa" to "🥫", "ketchup" to "🥫", "mustard" to "🥫",
    "mayo" to "🥫", "sauce" to "🥫", "gravy" to "🥫",
    "dressing" to "🥫", "salt" to "🧂", "olive" to "🫒",
    "oil" to "🫒"
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
