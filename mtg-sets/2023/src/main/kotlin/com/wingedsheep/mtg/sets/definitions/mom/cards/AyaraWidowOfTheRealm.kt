package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ayara, Widow of the Realm // Ayara, Furnace Queen (March of the Machine #90)
 * {1}{B}{B} Legendary Creature — Elf Noble 3/3 // Legendary Creature — Phyrexian Elf Noble 4/4
 *
 * Front — "{T}, Sacrifice another creature or artifact: deal X damage to target opponent or battle
 * and gain X life, where X is the sacrificed permanent's mana value." X is read off the cost's
 * last-known snapshot, so a token (mana value 0) deals and gains nothing.
 * "{5}{R/P}: Transform Ayara. Activate only as a sorcery."
 *
 * Back — at the beginning of combat on your turn, return up to one target artifact or creature card
 * from your graveyard; it gains haste and is exiled at the beginning of the next end step (anyone's).
 * The delayed exile is bound to the returned permanent, so a card that never arrived is not chased.
 */
private val AyaraWidowOfTheRealmFront = card("Ayara, Widow of the Realm") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Elf Noble"
    power = 3
    toughness = 3
    oracleText = "{T}, Sacrifice another creature or artifact: Ayara deals X damage to target opponent " +
        "or battle and you gain X life, where X is the sacrificed permanent's mana value.\n" +
        "{5}{R/P}: Transform Ayara. Activate only as a sorcery. " +
        "({R/P} can be paid with either {R} or 2 life.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact))
        val t = target(Targets.OpponentOrBattle)
        val x = DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0))
        effect = Effects.DealDamage(x, t) then Effects.GainLife(x)
        description = "Ayara deals X damage to target opponent or battle and you gain X life, " +
            "where X is the sacrificed permanent's mana value."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{R/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Ayara."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a21b1734-a773-4107-95c1-b44d5ccdfd82.jpg?1783917027"
    }
}

private val AyaraFurnaceQueen = card("Ayara, Furnace Queen") {
    manaCost = ""
    colorIndicator = "BR" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Phyrexian Elf Noble"
    power = 4
    toughness = 4
    oracleText = "At the beginning of combat on your turn, return up to one target artifact or creature " +
        "card from your graveyard to the battlefield. It gains haste. Exile it at the beginning of the " +
        "next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        target(
            TargetFilter(GameObjectFilter.CreatureOrArtifact.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true
        )
        effect = Effects.Pipeline {
            val returned = moveTracked(
                gather(CardSource.ChosenTargets),
                CardDestination.ToZone(Zone.BATTLEFIELD)
            )
            run(Effects.ForEachInCollection(
                collection = returned,
                effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent) then
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.Exile(EffectTarget.IterationEntity)
                    )
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Anna Podedworna"
        flavorText = "Ayara cherished her new machine servitors just as much as she once did her many " +
            "suitors: not at all."
        imageUri = "https://cards.scryfall.io/normal/back/a/2/a21b1734-a773-4107-95c1-b44d5ccdfd82.jpg?1783917027"
    }
}

val AyaraWidowOfTheRealm: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AyaraWidowOfTheRealmFront,
    backFace = AyaraFurnaceQueen,
)
