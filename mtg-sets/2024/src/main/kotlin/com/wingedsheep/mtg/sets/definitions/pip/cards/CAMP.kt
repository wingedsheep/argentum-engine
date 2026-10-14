package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * C.A.M.P. — Fallout #129
 * {3} · Artifact — Fortification
 *
 * Whenever fortified land is tapped for mana, put a +1/+1 counter on target creature you control.
 * If that creature shares a color with the mana that land produced, create a Junk token.
 * Fortify {3}
 *
 * "Fortified land is tapped for mana" is the ATTACHED form of the tapped-for-mana trigger — by any
 * player, since the passive voice names no tapper. The Junk rider reads the colors the land's mana
 * ability actually produced off the trigger, so colorless mana (or a non-matching color) gives only
 * the counter.
 */
val CAMP = card("C.A.M.P.") {
    manaCost = "{3}"
    typeLine = "Artifact — Fortification"
    oracleText = "Whenever fortified land is tapped for mana, put a +1/+1 counter on target creature you control. If that creature shares a color with the mana that land produced, create a Junk token. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")\nFortify {3} ({3}: Attach to target land you control. Fortify only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.attached.tappedForMana()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(
                    GameObjectFilter.Any.sharingColorWithTriggeringMana(),
                    creature
                ),
                then = Effects.CreateJunk()
            )
    }

    fortifyAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/034d4543-2c53-4cda-acfd-7f26b8dd17fb.jpg?1783912380"
        ruling("2024-03-08", "Fortification is to lands what Equipment is to creatures. Except for the difference regarding what type of permanent they can be attached to, Fortifications and Equipment follow the same rules.")
        ruling("2024-03-08", "If the fortified land produced more than one color of mana, you'll create a Junk token as long as the target creature is at least one of those colors.")
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
    }
}
