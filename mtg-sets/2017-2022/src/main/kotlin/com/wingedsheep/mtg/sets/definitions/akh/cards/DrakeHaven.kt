package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Drake Haven
 * {2}{U}
 * Enchantment
 * Whenever you cycle or discard a card, you may pay {1}. If you do, create a 2/2 blue Drake
 * creature token with flying.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card.
 */
val DrakeHaven = card("Drake Haven") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cycle or discard a card, you may pay {1}. If you do, create a 2/2 blue Drake creature token with flying."

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Drake"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/6/5/65844e72-aa84-483f-b07e-9e34e798aaec.jpg?1783936412"
            )
        )
        description = "Whenever you cycle or discard a card, you may pay {1}. If you do, create a 2/2 blue Drake creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "John Severin Brassell"
        flavorText = "\"If you want to truly test your survival skills, walk the Obelisk Path when the drakes are feeding.\"\n—Etha, initiate of Rhet crop"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4318da9d-dadf-444b-a0d4-fd270b216f5b.jpg?1783936524"
        ruling("2017-04-18", "While resolving Drake Haven's triggered ability, you can't pay {1} multiple times to get multiple Drakes.")
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
