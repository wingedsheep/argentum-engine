package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rodolf Duskbringer — Jumpstart 2022 #25
 * {5}{B} · Legendary Creature — Vampire Angel · 4/4
 *
 * Flying, deathtouch, lifelink
 * Whenever you gain life, Rodolf Duskbringer gains indestructible until end of turn.
 * At the beginning of your end step, you may pay {1}{W/B}. When you do, return target creature
 * card with mana value X or less from your graveyard to the battlefield, where X is the amount of
 * life you gained this turn.
 *
 * The end-step payment is a reflexive trigger (CR 603.12): the {1}{W/B} is paid as the end-step
 * trigger resolves, and only then does the "when you do" ability go on the stack and choose its
 * target. The cap is the total life gained this turn (not net of life lost, per the ruling), read
 * live by `manaValueAtMostDynamic(lifeGainedThisTurn)` at targeting and again on resolution.
 */
val RodolfDuskbringer = card("Rodolf Duskbringer") {
    manaCost = "{5}{B}"
    colorIdentity = "BW"
    typeLine = "Legendary Creature — Vampire Angel"
    power = 4
    toughness = 4
    oracleText = "Flying, deathtouch, lifelink\n" +
        "Whenever you gain life, Rodolf Duskbringer gains indestructible until end of turn.\n" +
        "At the beginning of your end step, you may pay {1}{W/B}. When you do, return target " +
        "creature card with mana value X or less from your graveyard to the battlefield, where X " +
        "is the amount of life you gained this turn."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}{W/B}"),
            descriptionOverride = "You may pay {1}{W/B}. When you do, return target creature card " +
                "with mana value X or less from your graveyard to the battlefield, where X is the " +
                "amount of life you gained this turn."
        ) {
            val creatureCard = target(
                TargetFilter(
                    GameObjectFilter.Creature.ownedByYou()
                        .manaValueAtMostDynamic(DynamicAmounts.lifeGainedThisTurn(Player.You)),
                    zone = Zone.GRAVEYARD,
                )
            )
            effect = Effects.PutOntoBattlefield(creatureCard)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9afd47b-ff94-4fdd-bb04-bdf9b8b61a6d.jpg?1783919187"
        ruling("2022-12-02", "X is the total amount of life you gained this turn, regardless of any life lost. For example, if you gained 3 life this turn and also lost 2 life this turn, X is 3, not 1.")
    }
}
