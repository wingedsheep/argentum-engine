package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Gobakhan // Lightshield Array — March of the Machine #22 (canonical printing).
 * {1}{W} · Battle — Siege · defense 3 // Enchantment
 *
 * When this Siege enters, look at target opponent's hand. You may exile a nonland card from it.
 * For as long as that card remains exiled, its owner may play it. A spell cast this way costs {2}
 * more to cast.
 *
 * The hand-look is Deep-Cavern Bat's pipeline (the `chooseUpTo(1)` is the printed "you may"); the
 * chosen card is then handed to [Effects.ExileAndGrantOwnerPlayPermission] (Soul Partition), which
 * exiles it and grants its *owner* a permanent play permission plus the {2} tax. The tax is
 * scoped to casters other than the Siege's controller, which here is every caster: the card came
 * from a target *opponent's* hand, so its owner is never you.
 *
 * The Siege reminder text restates rules every battle has, so only `startingDefense` and the back
 * face are declared (see Invasion of Innistrad).
 */
private val InvasionOfGobakhanFront = card("Invasion of Gobakhan") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Battle — Siege"
    startingDefense = 3
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, look at target opponent's hand. You may exile a nonland card " +
        "from it. For as long as that card remains exiled, its owner may play it. A spell cast " +
        "this way costs {2} more to cast."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LookAtHand(opponent) then
            Effects.Pipeline {
                val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val exiled = chooseUpTo(
                    1,
                    from = opponentHand,
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    prompt = "You may exile a nonland card from target opponent's hand",
                    showAllCards = true,
                    alwaysPrompt = true
                )
                ifNotEmpty(exiled) {
                    run(Effects.ExileAndGrantOwnerPlayPermission(
                        target = exiled.asTarget,
                        opponentCostIncrease = 2
                    ))
                }
            }
        description = "When this Siege enters, look at target opponent's hand. You may exile a " +
            "nonland card from it. For as long as that card remains exiled, its owner may play " +
            "it. A spell cast this way costs {2} more to cast."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11798730-6788-4e0b-a828-b46cab1a4fa7.jpg?1783917064"
        ruling("2023-04-14", "Playing the exiled card follows all normal timing restrictions.")
        ruling(
            "2023-04-14",
            "If the exiled card is a modal double-faced card and its back face is a land, its " +
                "owner may play it as a land. (Note that if the front face is also a land, you " +
                "couldn't have exiled it to begin with.)"
        )
    }
}

/**
 * The back face. Cast transformed, for free, by the Siege's defeat trigger — no mana cost, so a
 * white colour indicator.
 */
private val LightshieldArray = card("Lightshield Array") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, put a +1/+1 counter on each creature that " +
        "attacked this turn.\n" +
        "Sacrifice this enchantment: Creatures you control gain hexproof and indestructible " +
        "until end of turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.attackedThisTurn()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "At the beginning of your end step, put a +1/+1 counter on each creature " +
            "that attacked this turn."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Patterns.Group.grantKeywordToAll(Keyword.HEXPROOF, Filters.Group.creaturesYouControl) then
            Patterns.Group.grantKeywordToAll(Keyword.INDESTRUCTIBLE, Filters.Group.creaturesYouControl)
        description = "Sacrifice this enchantment: Creatures you control gain hexproof and " +
            "indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Andreas Zafiratos"
        flavorText = "The weight of Phyrexia crashed down upon Gobakhan, but the will of its " +
            "people endured."
        imageUri = "https://cards.scryfall.io/normal/back/1/1/11798730-6788-4e0b-a828-b46cab1a4fa7.jpg?1783917064"
        ruling(
            "2023-04-14",
            "The set of creatures affected by Lightshield Array's last ability is determined as " +
                "the ability resolves. Creatures you begin to control later in the turn and " +
                "noncreature permanents that become creatures later in the turn won't gain " +
                "hexproof and indestructible."
        )
    }
}

val InvasionOfGobakhan: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfGobakhanFront,
    backFace = LightshieldArray,
)
