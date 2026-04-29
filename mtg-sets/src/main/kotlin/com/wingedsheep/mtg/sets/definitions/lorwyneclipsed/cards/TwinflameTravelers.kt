package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Twinflame Travelers
 * {2}{U}{R}
 * Creature — Elemental Sorcerer
 * 3/3
 *
 * Flying
 * If a triggered ability of another Elemental you control triggers, it triggers an additional time.
 */
val TwinflameTravelers = card("Twinflame Travelers") {
    manaCost = "{2}{U}{R}"
    typeLine = "Creature — Elemental Sorcerer"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "If a triggered ability of another Elemental you control triggers, it triggers an additional time."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = GameObjectFilter.Creature.withSubtype("Elemental"),
            excludeSelf = true,
            description = "If a triggered ability of another Elemental you control triggers, it triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Jeff Miracola"
        flavorText = "\"Imagine the power of a flamekin and a rimekin united. Fervent passion confined by introspective insight. They would challenge every tale, rewrite every story.\"\n—Ashling"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fc9f409-3aef-4403-bf17-6e9a72ecfada.jpg?1767957280"
    }
}
