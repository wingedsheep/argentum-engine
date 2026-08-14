package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bespoke Battlegarb
 * {1}{R}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+0.
 * Celebration — At the beginning of combat on your turn, if two or more nonland permanents entered
 * the battlefield under your control this turn, attach this Equipment to up to one target creature
 * you control.
 * Equip {2}
 *
 * The Celebration trigger (CR 207.2c — italic flavor, no rules meaning) is an intervening-'if'
 * clause (CR 603.4) on [Conditions.Celebration], checked when the begin-combat step starts and
 * again on resolution. It is a *free* attach — no equip cost, no sorcery-speed restriction — so the
 * Battlegarb re-suits itself every combat in a turn that celebrates.
 *
 * "Up to one target" is an optional target ([TargetCreature] with `optional = true`), the same shape
 * as [LordSkitterSewerKing]'s exile: choosing no target leaves the Equipment where it is, which
 * matters because [Effects.AttachEquipment] would otherwise force a move off the creature it is
 * already on. Re-picking the current host is legal and a no-op (the executor suppresses the
 * "becomes attached" event when the host doesn't change, CR 603.2e).
 *
 * The printed [equipAbility] stays alongside it, so the Battlegarb is still equippable by hand at
 * sorcery speed on a turn that doesn't celebrate.
 */
val BespokeBattlegarb = card("Bespoke Battlegarb") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Celebration — At the beginning of combat on your turn, if two or more nonland permanents " +
        "entered the battlefield under your control this turn, attach this Equipment to up to one " +
        "target creature you control.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.Celebration
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.AttachEquipment(creature)
        description = "At the beginning of combat on your turn, if two or more nonland permanents " +
            "entered the battlefield under your control this turn, attach this Equipment to up to " +
            "one target creature you control."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a28ecd59-9166-473c-bafc-cb3c54c21388.jpg?1783915097"

        ruling(
            "2023-09-01",
            "Some celebration abilities trigger at specific parts of the turn and check whether " +
                "two or more nonland permanents entered the battlefield under your control " +
                "already in that turn."
        )
        ruling(
            "2023-09-01",
            "The permanents that entered the battlefield don't need to remain on the battlefield " +
                "or under your control. Celebration abilities are checking for past events, not " +
                "the current game state."
        )
    }
}
