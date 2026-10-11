package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scurry of Squirrels — Bloomburrow Commander #33
 * {2}{G} · Creature — Squirrel Scout · 2/2
 *
 * Myriad, myriad
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on target creature
 * you control.
 *
 * Two printed instances of the engine-live myriad keyword, so each attack offers two copies per
 * other opponent (CR 702.116b). The copies have myriad too, but they enter attacking and so never
 * trigger it; they do have the combat-damage trigger.
 */
val ScurryOfSquirrels = card("Scurry of Squirrels") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Squirrel Scout"
    power = 2
    toughness = 2
    oracleText = "Myriad, myriad (Whenever this creature attacks, for each opponent other than defending " +
        "player, you may create a token that's a copy of this creature that's tapped and attacking that " +
        "player or a planeswalker they control. Then do it again. Exile the tokens at end of combat.)\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on target creature " +
        "you control."

    keywordAbilities(KeywordAbility.Simple(Keyword.MYRIAD), KeywordAbility.Simple(Keyword.MYRIAD))

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c343b965-af2b-45c3-babb-9242736cf1e5.jpg?1783910727"
    }
}
