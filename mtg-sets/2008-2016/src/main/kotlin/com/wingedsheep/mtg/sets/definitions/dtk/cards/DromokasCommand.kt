package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dromoka's Command
 * {G}{W}
 * Instant
 *
 * Choose two —
 * • Prevent all damage target instant or sorcery spell would deal this turn.
 * • Target player sacrifices an enchantment of their choice.
 * • Put a +1/+1 counter on target creature.
 * • Target creature you control fights target creature you don't control.
 *
 * `modal(chooseCount = 2)` with every mode declaring its own targets, so only the chosen modes ask
 * for them (CR 601.2c).
 *
 * - The prevention is the source-keyed [Effects.PreventAllDamageDealtBy] over an instant-or-sorcery
 *   spell on the stack (Shieldmage Elder's spell ability): only damage the spell itself is the source
 *   of is prevented, so a fight the spell causes isn't touched — the first ruling.
 * - The third and fourth modes are separate instances of "target", so the same creature may get the
 *   counter and fight (CR 115.3). Modes resolve in printed order, so the counter is on it before it
 *   deals its fight damage.
 */
val DromokasCommand = card("Dromoka's Command") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Prevent all damage target instant or sorcery spell would deal this turn.\n" +
        "• Target player sacrifices an enchantment of their choice.\n" +
        "• Put a +1/+1 counter on target creature.\n" +
        "• Target creature you control fights target creature you don't control."

    spell {
        modal(chooseCount = 2) {
            mode("Prevent all damage target instant or sorcery spell would deal this turn") {
                val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
                effect = Effects.PreventAllDamageDealtBy(spell)
            }
            mode("Target player sacrifices an enchantment of their choice") {
                val player = target(Targets.Player)
                effect = Effects.Sacrifice(GameObjectFilter.Enchantment, 1, player)
            }
            mode("Put a +1/+1 counter on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            }
            mode("Target creature you control fights target creature you don't control") {
                val yourCreature = target(TargetFilter.CreatureYouControl)
                val theirCreature = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.Fight(yourCreature, theirCreature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df1e43a8-0adc-4e26-bc56-6b914bd35838.jpg?1783938573"
        ruling("2015-02-25", "Although most instants and sorceries that cause damage to be dealt are the sources of that damage, a few cause other objects to deal damage (for example, spells that cause creatures to fight). The first ability of Dromoka's Command won't prevent damage caused by those other sources.")
        ruling("2015-02-25", "You can target any player with the second ability, including one that doesn't control an enchantment.")
    }
}
