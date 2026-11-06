package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadowstorm Vizier
 * {U}{B}
 * Creature — Human Cleric
 * 1/3
 * Flying
 * Whenever you cycle or discard a card, this creature gets +1/+1 until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card, so
 * `Triggers.you.discards()` fires once per cycled card, per the ruling.
 */
val ShadowstormVizier = card("Shadowstorm Vizier") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Cleric"
    oracleText = "Flying\nWhenever you cycle or discard a card, this creature gets +1/+1 until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever you cycle or discard a card, this creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Yongjae Choi"
        flavorText = "She can coil a breeze like a string around her finger—or twist it into a noose."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea72b393-e47c-46de-a458-624ef0227486.jpg?1783936460"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
