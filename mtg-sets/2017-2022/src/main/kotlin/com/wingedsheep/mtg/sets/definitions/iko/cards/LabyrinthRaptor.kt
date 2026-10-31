package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Labyrinth Raptor — Ikoria: Lair of Behemoths #193
 * {B}{R} · Creature — Nightmare Dinosaur · 2/2
 *
 * Menace
 * Whenever a creature you control with menace becomes blocked, defending player sacrifices a
 * creature of their choice that's blocking it.
 * {B}{R}: Creatures you control with menace get +1/+0 until end of turn.
 *
 * The edict is restricted to the creatures blocking *that* attacker: they're gathered through
 * `blockingEntity(TriggeringEntity)` as the trigger resolves, and their controller — the defending
 * player, the only player whose creatures can block it — picks one to sacrifice. If every blocker
 * has already left, nothing is sacrificed. The remaining blocker keeps blocking (ruling).
 */
val LabyrinthRaptor = card("Labyrinth Raptor") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Nightmare Dinosaur"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "Whenever a creature you control with menace becomes blocked, defending player sacrifices a " +
        "creature of their choice that's blocking it.\n" +
        "{B}{R}: Creatures you control with menace get +1/+0 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.MENACE)).becomesBlocked()
        effect = Effects.Pipeline {
            val blockers = gather(GameObjectFilter.Creature.blockingEntity(EffectTarget.TriggeringEntity))
            val chosen = chooseExactly(
                1,
                from = blockers,
                chooser = Chooser.ControllerOfSelection,
                useTargetingUI = true,
                prompt = "Choose a creature blocking it to sacrifice"
            )
            sacrifice(chosen)
        }
        description = "Whenever a creature you control with menace becomes blocked, defending player " +
            "sacrifices a creature of their choice that's blocking it."
    }

    activatedAbility {
        cost = Costs.Mana("{B}{R}")
        effect = Patterns.Group.modifyStatsForAll(
            1,
            0,
            GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.MENACE))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c608543d-cb60-44c3-a437-e4a18c311420.jpg?1783931023"
        ruling(
            "2020-04-17",
            "Labyrinth Raptor's second and third abilities apply to itself as long as it still has menace."
        )
        ruling(
            "2020-04-17",
            "The defending player sacrifices a blocking creature before combat damage is dealt. This may cause " +
                "a single creature to be blocking the creature with menace; the blocking creature remains " +
                "blocking. It will still deal and be dealt combat damage as normal."
        )
        ruling(
            "2020-04-17",
            "If a creature is attacking a planeswalker, the planeswalker's controller is the defending player."
        )
    }
}
