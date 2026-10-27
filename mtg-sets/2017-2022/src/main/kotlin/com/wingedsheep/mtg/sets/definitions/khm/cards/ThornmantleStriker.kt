package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Thornmantle Striker
 * {4}{B}
 * Creature — Elf Rogue
 * 4/3
 * When this creature enters, choose one —
 * • Remove X counters from target permanent, where X is the number of Elves you control.
 * • Target creature an opponent controls gets -X/-X until end of turn, where X is the number of
 *   Elves you control.
 *
 * Cemetery Desecrator's two modes with an Elf tally for X. X is read as the trigger resolves,
 * counting Striker itself if it's still under your control (2021-02-05 ruling). The removal mode is
 * [Effects.RemoveCounterOfAnyKind] with floor = ceiling = X: the player picks which kinds come off,
 * and a permanent with fewer than X counters loses all of them.
 */
val ThornmantleStriker = card("Thornmantle Striker") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Rogue"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, choose one —\n" +
        "• Remove X counters from target permanent, where X is the number of Elves you control.\n" +
        "• Target creature an opponent controls gets -X/-X until end of turn, where X is the number of Elves you control."

    val elves = DynamicAmounts.battlefield(
        Player.You,
        GameObjectFilter.Permanent.withSubtype(Subtype.ELF)
    ).count()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Remove X counters from target permanent, where X is the number of Elves you control") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.RemoveCounterOfAnyKind(target = permanent, count = elves)
            },
            mode("Target creature an opponent controls gets -X/-X until end of turn, where X is the number of Elves you control") {
                val creature = target(TargetFilter.Creature.opponentControls())
                effect = Effects.ModifyStats(-elves, -elves, creature)
            },
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "387"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/544b6477-cefa-40e2-a96a-72794c7eb815.jpg?1783928117"
        ruling("2021-02-05", "For each mode, use the number of Elves you control as the enters-the-battlefield ability resolves, including Thornmantle Striker itself if it's still under your control.")
        ruling("2021-02-05", "For the first mode, you choose which counters are removed as the ability resolves. If the permanent has fewer counters on it than the number of Elves you control, you'll remove all of them.")
        ruling("2021-02-05", "A planeswalker with no loyalty counters on it is put into its owner's graveyard.")
    }
}
