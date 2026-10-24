package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Kianne, Dean of Substance // Imbraham, Dean of Theory — Strixhaven: School of Mages #152
 * (canonical printing)
 *
 * A modal double-faced card (CR 712) whose faces are both creatures — the
 * [CardDefinition.modalDoubleFacedPermanent] shape (Valentin, Dean of the Vein).
 *
 * Both faces share [CounterType.STUDY], a mark on cards in exile. Every reader looks at all the
 * cards you *own* in exile with a study counter, whichever permanent put it there (ruling), via
 * `withCounter(STUDY)` over the exile zone; the counters fall off when a card leaves exile.
 *
 * Front — Kianne ({2}{G}, 2/2): "{T}: exile the top card; land → hand, otherwise a study
 * counter" exiles first, then splits the exiled card by type. "{4}{G}: Fractal token with a
 * +1/+1 counter for each different mana value among nonland study cards" counts distinct mana
 * values over owned exile as the ability resolves.
 *
 * Back — Imbraham ({2}{U}{U}, 3/3 flier): "{X}{U}{U}, {T}" exiles the top X with a study counter
 * each, then offers up to one study card you own in exile — the freshly exiled ones included,
 * and still offered when X is 0 (ruling).
 */
private val nonlandStudyCard = GameObjectFilter.Nonland.withCounter(CounterType.STUDY)

private val KianneDeanOfSubstanceFront = card("Kianne, Dean of Substance") {
    manaCost = "{2}{G}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Druid"
    power = 2
    toughness = 2
    oracleText = "{T}: Exile the top card of your library. If it's a land card, put it into your hand. " +
        "Otherwise, put a study counter on it.\n" +
        "{4}{G}: Create a 0/0 green and blue Fractal creature token. Put a +1/+1 counter on it for each " +
        "different mana value among nonland cards you own in exile with study counters on them."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
            move(top, CardDestination.ToZone(Zone.EXILE))
            val (lands, nonlands) = filterSplit(top, GameObjectFilter.Land)
            toHand(lands)
            run(Effects.AddCountersToCollection(nonlands, CounterType.STUDY))
        }
        description = "{T}: Exile the top card of your library. If it's a land card, put it into your " +
            "hand. Otherwise, put a study counter on it."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{G}")
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value),
        ) then Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.zone(Player.You, Zone.EXILE, nonlandStudyCard)
                .distinctValues(CardNumericProperty.MANA_VALUE),
            EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
        )
        description = "{4}{G}: Create a 0/0 green and blue Fractal creature token. Put a +1/+1 counter on " +
            "it for each different mana value among nonland cards you own in exile with study counters on them."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b45dc40-6827-46a7-a9b7-802be698d053.jpg?1783927335"
        ruling("2021-04-16", "Cards are exiled face up unless an effect states otherwise.")
        ruling(
            "2021-04-16",
            "Exiled cards with study counters on them retain their study counters even if Kianne or Imbraham " +
                "leave the battlefield.",
        )
        ruling(
            "2021-04-16",
            "The second abilities of both Kianne and Imbraham look at all cards you own in exile with study " +
                "counters, even if they were exiled by a different permanent, such as another Kianne or Imbraham.",
        )
    }
}

private val ImbrahamDeanOfTheoryBack = card("Imbraham, Dean of Theory") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Bird Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "{X}{U}{U}, {T}: Exile the top X cards of your library and put a study counter on each of them. " +
        "Then you may put a card you own in exile with a study counter on it into your hand."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{U}{U}"), Costs.Tap)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue()))
            move(top, CardDestination.ToZone(Zone.EXILE), addCounterType = CounterType.STUDY)
            val studied = gather(
                CardSource.FromZone(
                    zone = Zone.EXILE,
                    player = Player.You,
                    filter = GameObjectFilter.Any.withCounter(CounterType.STUDY),
                )
            )
            val chosen = chooseUpTo(
                1,
                from = studied,
                prompt = "You may put a card with a study counter on it into your hand",
                selectedLabel = "Put into hand",
            )
            toHand(chosen)
        }
        description = "{X}{U}{U}, {T}: Exile the top X cards of your library and put a study counter on each " +
            "of them. Then you may put a card you own in exile with a study counter on it into your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/back/8/b/8b45dc40-6827-46a7-a9b7-802be698d053.jpg?1783927335"
        ruling("2021-04-16", "Cards are exiled face up unless an effect states otherwise.")
        ruling(
            "2021-04-16",
            "The second abilities of both Kianne and Imbraham look at all cards you own in exile with study " +
                "counters, even if they were exiled by a different permanent, such as another Kianne or Imbraham.",
        )
        ruling(
            "2021-04-16",
            "You may choose 0 as the value of X as you activate Imbraham's last ability. If you do, you will " +
                "not exile any cards from your library. However, you may still put a card previously exiled with " +
                "a study counter on it into your hand.",
        )
    }
}

val KianneDeanOfSubstance: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = KianneDeanOfSubstanceFront,
    backFace = ImbrahamDeanOfTheoryBack,
)
