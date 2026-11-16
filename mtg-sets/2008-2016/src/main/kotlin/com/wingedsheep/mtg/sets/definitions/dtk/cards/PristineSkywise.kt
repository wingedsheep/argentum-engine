package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pristine Skywise
 * {4}{W}{U}
 * Creature — Dragon
 * 6/4
 * Flying
 * Whenever you cast a noncreature spell, untap this creature. It gains protection from the color
 * of your choice until end of turn.
 *
 * The trigger is Zephyr Scribe's untap composed with Kami of the Painted Road's grant: the color is
 * chosen as the trigger resolves (it doesn't target), so `ChooseColorThen` wraps the protection grant.
 */
val PristineSkywise = card("Pristine Skywise") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast a noncreature spell, untap this creature. It gains protection from the color of your choice until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.Untap(EffectTarget.Self) then
            Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Adam Paquette"
        flavorText = "The elite of Ojutai's brood, the skywise see their enemies as puzzles to be solved."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/083eabac-f7c9-45d9-b3c7-10d719da1e0b.jpg?1783938571"
    }
}
