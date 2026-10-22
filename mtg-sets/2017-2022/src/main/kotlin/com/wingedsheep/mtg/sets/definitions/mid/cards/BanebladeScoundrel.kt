package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baneblade Scoundrel // Baneclaw Marauder (Innistrad: Midnight Hunt)
 * {3}{B}
 * Creature — Human Rogue Werewolf // Creature — Werewolf
 *
 * Front — Baneblade Scoundrel (4/3): "Whenever this creature becomes blocked, each creature blocking
 *          it gets -1/-1 until end of turn." Daybound.
 * Back  — Baneclaw Marauder (5/4): the same block trigger, plus "Whenever a creature blocking this
 *          creature dies, that creature's controller loses 1 life." Nightbound.
 *
 * The block trigger is Battle-Scarred Goblin's `becomesBlocked()` over the source-relative
 * `blockingSource()` group. The death trigger gates on the same filter, which a dying blocker
 * answers from its last-known combat pairing (CR 603.10) — so a blocker the -1/-1 kills triggers
 * it too. "That creature's controller" is the dead creature's last-known controller.
 */

private const val BLOCK_TRIGGER_TEXT =
    "Whenever this creature becomes blocked, each creature blocking it gets -1/-1 until end of turn."

private const val DIES_TRIGGER_TEXT =
    "Whenever a creature blocking this creature dies, that creature's controller loses 1 life."

private val BanebladeScoundrelFront = card("Baneblade Scoundrel") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue Werewolf"
    power = 4
    toughness = 3
    oracleText = "$BLOCK_TRIGGER_TEXT\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter(GameObjectFilter.Creature.blockingSource()))
        description = BLOCK_TRIGGER_TEXT
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b63f2ae-5bfd-452f-b1f5-8459bcecd3bb.jpg?1783925632"
    }
}

private val BaneclawMarauder = card("Baneclaw Marauder") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 4
    oracleText = "$BLOCK_TRIGGER_TEXT\n" +
        "$DIES_TRIGGER_TEXT\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter(GameObjectFilter.Creature.blockingSource()))
        description = BLOCK_TRIGGER_TEXT
    }
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.blockingSource()).dies()
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity)
        description = DIES_TRIGGER_TEXT
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/back/7/b/7b63f2ae-5bfd-452f-b1f5-8459bcecd3bb.jpg?1783925632"
    }
}

val BanebladeScoundrel: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BanebladeScoundrelFront,
    backFace = BaneclawMarauder,
)
