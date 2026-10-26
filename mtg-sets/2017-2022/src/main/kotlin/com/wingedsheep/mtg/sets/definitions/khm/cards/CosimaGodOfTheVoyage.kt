package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cosima, God of the Voyage // The Omenkeel — Kaldheim #50 (canonical printing)
 * Legendary Creature — God 2/4 // Legendary Artifact — Vehicle 3/3 · Modal double-faced card
 *
 * Front: Cosima exiles herself and *gains*, in exile, the quoted landfall ability — the Uvilda,
 * Dean of Perfection shape (`GrantTriggeredAbility` onto the exiled card, `Duration.Permanent`,
 * ended by leaving exile; a Cosima exiled any other way is a new object without it). "If you
 * don't" is the `May`'s `otherwise`: the voyage count is stored before the return, because the
 * counters fall off as she leaves exile, and her +1/+1 counters are entry counters read the same way.
 *
 * Back: "that player" is the damaged player; only the land cards among the exiled cards get the
 * play permission, which lasts while they stay exiled (the ruling: even if The Omenkeel leaves).
 */
private val CosimaGodOfTheVoyageFront = card("Cosima, God of the Voyage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — God"
    power = 2
    toughness = 4
    oracleText = "At the beginning of your upkeep, you may exile Cosima. If you do, it gains \"Whenever a land you " +
        "control enters, if Cosima is exiled, you may put a voyage counter on it. If you don't, return Cosima to " +
        "the battlefield with X +1/+1 counters on it and draw X cards, where X is the number of voyage counters on it.\""

    val voyage = grantedTriggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        triggerZone = Zone.EXILE
        interveningIf = Conditions.SourceInZone(Zone.EXILE)
        effect = Effects.May(
            effect = Effects.AddCounters(CounterType.VOYAGE, 1, EffectTarget.Self),
            otherwise = Effects.Pipeline {
                val voyageCount = storeNumber(DynamicAmounts.countersOnSelf(CounterType.VOYAGE))
                run(
                    Effects.Move(
                        EffectTarget.Self,
                        Zone.BATTLEFIELD,
                        fromZone = Zone.EXILE,
                        entryCounters = listOf(EntryCounter(CounterType.PLUS_ONE_PLUS_ONE, count = voyageCount.amount)),
                    )
                )
                run(Effects.DrawCards(voyageCount.amount))
            },
            prompt = "Put a voyage counter on Cosima? If you don't, she returns to the battlefield.",
        )
        description = "Whenever a land you control enters, if Cosima is exiled, you may put a voyage counter on it. " +
            "If you don't, return Cosima to the battlefield with X +1/+1 counters on it and draw X cards, where X " +
            "is the number of voyage counters on it."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.IfYouDo(
                Effects.Exile(EffectTarget.Self, fromZone = Zone.BATTLEFIELD),
                Effects.GrantTriggeredAbility(voyage, EffectTarget.Self, Duration.Permanent),
            ),
            prompt = "Exile Cosima?",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Micah Epstein"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fab2fca4-a99f-4ffe-9c02-edb6e0be2358.jpg?1783928272"
        ruling("2021-02-05", "Cosima's ability triggers only if Cosima is on the battlefield at the beginning of your upkeep, and you may exile Cosima only if it is still on the battlefield as the ability resolves.")
        ruling("2021-02-05", "While the triggered ability Cosima gains is resolving, if you don't put a voyage counter on Cosima, you'll immediately return it to the battlefield and draw cards equal to the number of voyage counters Cosima had on it in exile. Players can't respond to this choice, and no player may take actions during this process.")
        ruling("2021-02-05", "You may choose to return Cosima to the battlefield even if it doesn't have any voyage counters on it. If you do, it won't enter with any +1/+1 counters on it and you won't draw any cards.")
        ruling("2021-02-05", "If a land enters the battlefield under your control while Cosima is exiled and has the added triggered ability, that ability will trigger. As the ability tries to resolve, it will check to see if Cosima is still in exile. If Cosima has left exile before the ability tries to resolve, it won't resolve and none of its effects will happen. This is true even if Cosima returns to exile before that ability tries to resolve.")
    }
}

private val TheOmenkeelBack = card("The Omenkeel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Whenever a Vehicle you control deals combat damage to a player, that player exiles that many cards " +
        "from the top of their library. You may play lands from among those cards for as long as they remain exiled.\n" +
        "Crew 1"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl())
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.triggerDamageAmount(), Player.TriggeringPlayer))
            exile(top, owner = Player.TriggeringPlayer)
            val lands = filter(top, GameObjectFilter.Land)
            run(Effects.GrantMayPlayFromExile(from = lands, expiry = MayPlayExpiry.Permanent))
        }
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Micah Epstein"
        imageUri = "https://cards.scryfall.io/normal/back/f/a/fab2fca4-a99f-4ffe-9c02-edb6e0be2358.jpg?1783928272"
        ruling("2021-02-05", "The Omenkeel doesn't allow you to play additional lands during your turn, nor does it allow you to play lands at times you normally can't. In most cases, you'll be able to play only one land from among cards exiled with The Omenkeel during each of your turns.")
        ruling("2021-02-05", "You can play lands from among cards exiled with The Omenkeel for as long as they remain exiled, even if The Omenkeel leaves the battlefield or another player gains control of it.")
    }
}

val CosimaGodOfTheVoyage: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = CosimaGodOfTheVoyageFront,
    backFace = TheOmenkeelBack,
)
