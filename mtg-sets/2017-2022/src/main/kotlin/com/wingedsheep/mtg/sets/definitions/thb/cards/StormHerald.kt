package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm Herald
 * {2}{R}
 * Creature — Human Shaman
 * 3/2
 * Haste
 * When this creature enters, return any number of Aura cards from your graveyard to the
 * battlefield attached to creatures you control. Exile those Auras at the beginning of your next
 * end step. If those Auras would leave the battlefield, exile them instead of putting them
 * anywhere else.
 *
 * The chosen Auras enter together, each attached to a creature you control that you pick as it
 * enters (`auraHostFilter`; Storm Herald itself qualifies). One that can't enchant any creature you
 * control stays in the graveyard (ruling), so the tracked `returned` slot holds only the Auras that
 * actually came back — the delayed exile and the exile-on-leave replacement touch nothing else.
 */
val StormHerald = card("Storm Herald") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "When this creature enters, return any number of Aura cards from your graveyard to the battlefield " +
        "attached to creatures you control. Exile those Auras at the beginning of your next end step. If " +
        "those Auras would leave the battlefield, exile them instead of putting them anywhere else."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val auras = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Enchantment.withSubtype(Subtype.AURA))
            )
            val chosen = chooseAnyNumber(auras, prompt = "Choose any number of Aura cards to return")
            val returned = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                auraHostFilter = GameObjectFilter.Creature.youControl(),
                name = "returned"
            )
            run(Effects.ForEachInCollection(returned, Effects.GrantExileOnLeave(EffectTarget.IterationEntity)))
            run(
                Effects.CreateDelayedTrigger(
                    step = Step.END,
                    fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                    timing = DelayedTriggerTiming.NEXT_END_STEP,
                    effect = Effects.Pipeline { exile(returned) },
                    carryCollections = listOf(returned.key),
                )
            )
        }
        description = "When this creature enters, return any number of Aura cards from your graveyard to the " +
            "battlefield attached to creatures you control. Exile those Auras at the beginning of your next end " +
            "step. If those Auras would leave the battlefield, exile them instead of putting them anywhere else."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0594aa2-1c53-4271-a0d0-9002c07f3697.jpg?1783931544"
        ruling("2020-01-24", "Storm Herald itself is among the creatures that may be enchanted by the returning Auras.")
        ruling("2020-01-24", "If an Aura can't be attached to a creature you control, you can't return it to the battlefield.")
        ruling(
            "2020-01-24",
            "Only Auras that were returned to the battlefield will be exiled at the beginning of your next end step. " +
                "Any that you choose to leave in your graveyard will remain."
        )
        ruling(
            "2020-01-24",
            "If an effect exiles an Aura returned this way, that effect exiles it, not Storm Herald's effect. If that " +
                "effect returns the Aura to the battlefield, it won't be affected by Storm Herald's effect anymore."
        )
    }
}
