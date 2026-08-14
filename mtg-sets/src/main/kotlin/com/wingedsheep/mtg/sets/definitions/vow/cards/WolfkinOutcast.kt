package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wolfkin Outcast // Wedding Crasher (Innistrad: Crimson Vow)
 * {5}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Wolfkin Outcast (5/4): "This spell costs {2} less to cast if you control a Wolf or Werewolf";
 *          Daybound.
 * Back  — Wedding Crasher (6/5): "Whenever this creature or another Wolf or Werewolf you control dies,
 *          draw a card"; Nightbound.
 *
 * The front's cost reduction is the Academy Journeymage rail — a [ModifySpellCost] over
 * [SpellCostTarget.SelfCast] with [CostReductionSource.FixedIfControlFilter], here reducing generic by 2
 * when you control any Wolf *or* Werewolf (`GameObjectFilter.Any.withAnySubtype("Wolf", "Werewolf")`).
 *
 * The back's death payoff is the Archghoul of Thraben rail — "this creature **or another** [subtype] you
 * control dies" is [Triggers.leavesBattlefield] to the graveyard, filtered to a Wolf-or-Werewolf you
 * control, with [TriggerBinding.ANY] so the source counts itself. Per the werewolf-set rulings, a
 * simultaneous death of Wedding Crasher plus another Wolf/Werewolf triggers once per dying permanent (the
 * per-event `ZoneChangeEvent`, not a batch), so a two-Wolf wipe draws two cards.
 *
 * The back is a transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "G"`.
 */

private val WOLF_OR_WEREWOLF = GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf").youControl()

private val WolfkinOutcastFront = card("Wolfkin Outcast") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 5
    toughness = 4
    oracleText = "This spell costs {2} less to cast if you control a Wolf or Werewolf.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(
                    amount = 2,
                    filter = GameObjectFilter.Any.withAnySubtype("Wolf", "Werewolf"),
                ),
            ),
        )
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a743426-6333-4ca6-9207-163b325ba435.jpg?1783924803"
    }
}

private val WeddingCrasher = card("Wedding Crasher") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 5
    oracleText = "Whenever this creature or another Wolf or Werewolf you control dies, draw a card.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = WOLF_OR_WEREWOLF,
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.DrawCards(1, EffectTarget.Controller)
        description = "Draw a card."
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/back/7/a/7a743426-6333-4ca6-9207-163b325ba435.jpg?1783924803"
    }
}

val WolfkinOutcast: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = WolfkinOutcastFront,
    backFace = WeddingCrasher,
)
