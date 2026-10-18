package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Fall of Lord Konda // Fragment of Konda
 * {2}{W} — Enchantment — Saga // Enchantment Creature — Human Noble 1/3
 *
 * Front — The Fall of Lord Konda:
 *   I — Exile target creature an opponent controls with mana value 4 or greater.
 *   II — Each player gains control of all permanents they own.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Fragment of Konda:
 *   Defender
 *   When this creature dies, draw a card.
 *
 * Chapter II has no owner-of-the-iterated-permanent player reference, so the loop is turned
 * inside out (as on Cut the Tethers): `ForEachPlayer(Each)` rebinds "you" to each player, and the
 * inner `ForEachInGroup` hands that player control of every permanent they own. Each grant is a
 * new control effect with a fresh timestamp, so it overrides older control effects — floating
 * ones and static ones such as a stolen permanent's Confiscate (CR 613.7) — while a permanent
 * already under its owner's control is left untouched.
 */
private val FragmentOfKonda = card("Fragment of Konda") {
    manaCost = ""
    colorIndicator = "W"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Noble"
    oracleText = "Defender\nWhen this creature dies, draw a card."
    power = 1
    toughness = 3

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Andrew Mar"
        flavorText = "For his transgressions against the kami, Lord Konda's immortal body was turned to " +
            "stone, then shattered into a thousand still-living pieces."
        imageUri = "https://cards.scryfall.io/normal/back/4/d/4d784409-df6d-45d4-8f8b-6e3fd57a8fde.jpg?1783923930"
    }
}

private val TheFallOfLordKondaFront = card("The Fall of Lord Konda") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Exile target creature an opponent controls with mana value 4 or greater.\n" +
        "II — Each player gains control of all permanents they own.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls().manaValueAtLeast(4)))
        effect = Effects.Exile(creature)
    }

    sagaChapter(2) {
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Permanent.ownedByYou()),
                effect = Effects.GainControl(EffectTarget.IterationEntity),
            ),
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d784409-df6d-45d4-8f8b-6e3fd57a8fde.jpg?1783923930"
    }
}

val TheFallOfLordKonda: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheFallOfLordKondaFront,
    backFace = FragmentOfKonda,
)
