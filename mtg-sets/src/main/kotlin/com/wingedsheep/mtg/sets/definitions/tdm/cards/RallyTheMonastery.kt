package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rally the Monastery — Tarkir: Dragonstorm #19
 * {3}{W} · Instant
 *
 * This spell costs {2} less to cast if you've cast another spell this turn.
 * Choose one —
 * • Create two 1/1 white Monk creature tokens with prowess.
 * • Up to two target creatures you control each get +2/+2 until end of turn.
 * • Destroy target creature with power 4 or greater.
 *
 * The cost reduction is evaluated at cast time, before this spell is counted, so
 * `YouCastSpellsThisTurn(atLeast = 1)` means "you've cast another spell this turn".
 */
val RallyTheMonastery = card("Rally the Monastery") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if you've cast another spell this turn.\n" +
        "Choose one —\n" +
        "• Create two 1/1 white Monk creature tokens with prowess.\n" +
        "• Up to two target creatures you control each get +2/+2 until end of turn.\n" +
        "• Destroy target creature with power 4 or greater."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.YouCastSpellsThisTurn(atLeast = 1)),
        )
    }

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                effect = CreateTokenEffect(
                    count = 2,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Monk"),
                    keywords = setOf(Keyword.PROWESS),
                    name = "Monk",
                    imageUri = "https://cards.scryfall.io/normal/front/6/3/633d2d10-def7-426f-8496-ed6b45684299.jpg?1742421122"
                ),
                description = "Create two 1/1 white Monk creature tokens with prowess"
            ),
            Mode.withTarget(
                effect = ForEachTargetEffect(
                    listOf(Effects.ModifyStats(power = 2, toughness = 2, target = EffectTarget.ContextTarget(0)))
                ),
                target = TargetCreature(count = 2, minCount = 0, filter = TargetFilter.CreatureYouControl),
                description = "Up to two target creatures you control each get +2/+2 until end of turn"
            ),
            Mode.withTarget(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                target = TargetCreature(filter = TargetFilter.Creature.powerAtLeast(4)),
                description = "Destroy target creature with power 4 or greater"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b56e0037-8143-4c13-83e1-0c3f44e685ea.jpg?1743204029"
    }
}
