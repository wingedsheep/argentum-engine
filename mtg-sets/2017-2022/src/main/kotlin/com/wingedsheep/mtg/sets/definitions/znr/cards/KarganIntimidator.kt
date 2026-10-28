package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kargan Intimidator
 * {1}{R}
 * Creature — Human Warrior
 * 3/1
 *
 * Cowards can't block Warriors.
 * {1}: Choose one that hasn't been chosen this turn —
 * • This creature gets +1/+1 until end of turn.
 * • Target creature becomes a Coward until end of turn.
 * • Target Warrior gains trample until end of turn.
 *
 * "Cowards can't block Warriors" is read from the attacker's side: every Warrior (any controller)
 * can't be blocked by Cowards. "Becomes a Coward" replaces the creature's other creature types
 * (CR 205.1b), so it is [Effects.SetCreatureSubtypes], not an added type. "Choose one that hasn't
 * been chosen this turn" is [ModalEffect.chooseOneNotYetChosenThisTurn]: the mode is announced at
 * activation and remembered per source object for the turn, so each mode can be activated once per
 * turn per Intimidator.
 */
val KarganIntimidator = card("Kargan Intimidator") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 1
    oracleText = "Cowards can't block Warriors.\n" +
        "{1}: Choose one that hasn't been chosen this turn —\n" +
        "• This creature gets +1/+1 until end of turn.\n" +
        "• Target creature becomes a Coward until end of turn.\n" +
        "• Target Warrior gains trample until end of turn."

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withSubtype(Subtype("Coward")),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR))
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = ModalEffect.chooseOneNotYetChosenThisTurn(
            Mode.noTarget(
                Effects.ModifyStats(1, 1, EffectTarget.Self),
                "This creature gets +1/+1 until end of turn"
            ),
            mode("Target creature becomes a Coward until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.SetCreatureSubtypes(setOf("Coward"), creature, Duration.EndOfTurn)
            },
            mode("Target Warrior gains trample until end of turn") {
                val warrior = target(TargetFilter.Creature.withSubtype(Subtype.WARRIOR))
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, warrior)
            }
        )
        description = "{1}: Choose one that hasn't been chosen this turn — this creature gets +1/+1; " +
            "or target creature becomes a Coward; or target Warrior gains trample (until end of turn)."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0885acdc-9b4c-4e28-8785-9e721a27e81e.jpg?1783929359"

        ruling("2020-09-25", "Once a creature has blocked a Warrior, turning that creature into a Coward won't remove it from combat or cause the Warrior to become unblocked.")
        ruling("2020-09-25", "If you control two or more Kargan Intimidators, track which modes have been chosen each turn for each one's ability separately.")
    }
}
