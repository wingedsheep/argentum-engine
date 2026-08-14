package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Howler's Heavy — Aetherdrift #46
 * {3}{U} · Creature — Seal Pirate · 3/4
 *
 * Cycling {1}{U}
 * When you cycle this card, target creature or Vehicle an opponent controls gets -3/-0 until end
 * of turn.
 *
 * The cycling trigger ([Triggers.YouCycleThis]) fires from the graveyard after the cycling ability
 * has already resolved, and it targets on the way to the stack — so it can be responded to, and it
 * simply fizzles if the chosen permanent leaves before resolution.
 *
 * A Vehicle matches [GameObjectFilter.CreatureOrVehicle] by subtype whether or not it is currently
 * crewed, which is what the printed "creature or Vehicle" wants: -3/-0 applied to an uncrewed
 * Vehicle still shrinks it once it is crewed later this turn.
 */
val HowlersHeavy = card("Howler's Heavy") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Seal Pirate"
    power = 3
    toughness = 4
    oracleText = "Cycling {1}{U} ({1}{U}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, target creature or Vehicle an opponent controls gets -3/-0 " +
        "until end of turn."

    keywordAbility(KeywordAbility.cycling("{1}{U}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val victim = target(
            "target creature or Vehicle an opponent controls",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle.opponentControls()))
        )
        effect = Effects.ModifyStats(-3, 0, victim)
        description = "When you cycle this card, target creature or Vehicle an opponent controls " +
            "gets -3/-0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Borja Pindado"
        flavorText = "\"It takes a thick skin to ride among the Keelhaulers.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bbd7758-59c7-4ae1-9af8-c3580f4aa958.jpg?1783907909"
    }
}
