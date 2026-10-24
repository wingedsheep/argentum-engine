package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.ReplacementRiderTiming
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valentin, Dean of the Vein // Lisette, Dean of the Root — Strixhaven: School of Mages #161
 * (canonical printing)
 *
 * A modal double-faced card (CR 712) whose faces are both creatures — the
 * [CardDefinition.modalDoubleFacedPermanent] shape (Plargg, Dean of Chaos).
 *
 * Front — Valentin, Dean of the Vein ({B}, Legendary Creature — Vampire Warlock, 1/1):
 *  - Menace, lifelink.
 *  - "If a nontoken creature an opponent controls would die, exile it instead. When you do, you may
 *    pay {2}. If you do, create a 1/1 black and green Pest creature token with 'When this token
 *    dies, you gain 1 life.'" Nemata, Primeval Warden's death shield: a [RedirectZoneChangeWith]
 *    whose rider is a reflexive trigger ([ReplacementRiderTiming.WHEN_YOU_DO], CR 603.12), so it
 *    only exists when Valentin's replacement actually applied (ruling), and resolves as a
 *    [Effects.MayPay] {2} gate over [Effects.CreatePest].
 *
 * Back — Lisette, Dean of the Root ({2}{G}{G}, Legendary Creature — Human Druid, 4/4):
 *  - "Whenever you gain life, you may pay {1}. If you do, put a +1/+1 counter on each creature you
 *    control and those creatures gain trample until end of turn." One trigger per life-gain event
 *    (ruling); the {1} is paid at most once per trigger. Each creature that gets a counter also gets
 *    its own end-of-turn trample grant via [Effects.ForEachInGroup], so the affected set is fixed
 *    at resolution.
 */
private val ValentinDeanOfTheVeinFront = card("Valentin, Dean of the Vein") {
    manaCost = "{B}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Vampire Warlock"
    power = 1
    toughness = 1
    oracleText = "Menace, lifelink\n" +
        "If a nontoken creature an opponent controls would die, exile it instead. When you do, you may pay " +
        "{2}. If you do, create a 1/1 black and green Pest creature token with \"When this token dies, you " +
        "gain 1 life.\""

    keywords(Keyword.MENACE, Keyword.LIFELINK)

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.MayPay(ManaCost.parse("{2}"), Effects.CreatePest()),
            riderTiming = ReplacementRiderTiming.WHEN_YOU_DO,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken().opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65008352-bc7e-40b2-a832-b46813e5dc4c.jpg?1783927330"
        ruling(
            "2021-04-16",
            "If a nontoken creature an opponent controls would die, and both Valentin and another effect are " +
                "trying to exile it instead, that opponent chooses which effect to apply. If Valentin's effect " +
                "ends up not applying because of this choice, the triggered ability that allows you to pay {2} " +
                "won't trigger, even if the creature card ends up in exile.",
        )
    }
}

private val LisetteDeanOfTheRootBack = card("Lisette, Dean of the Root") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Druid"
    power = 4
    toughness = 4
    oracleText = "Whenever you gain life, you may pay {1}. If you do, put a +1/+1 counter on each creature " +
        "you control and those creatures gain trample until end of turn."

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.MayPay(
            ManaCost.parse("{1}"),
            Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity, Duration.EndOfTurn),
            ),
        )
        description = "Whenever you gain life, you may pay {1}. If you do, put a +1/+1 counter on each " +
            "creature you control and those creatures gain trample until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Jesper Ejsing"
        flavorText = "\"Life is as fleeting as it is vibrant. Embrace the brief blessing of a flower before it " +
            "withers.\""
        imageUri = "https://cards.scryfall.io/normal/back/6/5/65008352-bc7e-40b2-a832-b46813e5dc4c.jpg?1783927330"
        ruling(
            "2021-04-16",
            "If you gain life at the same time a creature you control is dealt lethal damage, that creature " +
                "dies before Lisette's ability can put a +1/+1 counter on it.",
        )
        ruling(
            "2021-04-16",
            "An ability that triggers \"whenever you gain life\" triggers just once for each life-gaining " +
                "event, no matter how much life you gain.",
        )
        ruling("2021-04-16", "You may only pay {1} once for each life gain event.")
        ruling(
            "2021-04-16",
            "Each creature with lifelink dealing combat damage causes a separate life-gaining event. For " +
                "example, if two creatures you control with lifelink deal combat damage at the same time, a " +
                "\"whenever you gain life\" ability will trigger twice. However, if a single creature you " +
                "control with lifelink deals combat damage to multiple creatures, players, and/or planeswalkers " +
                "at the same time (perhaps because it has trample or was blocked by more than one creature), " +
                "the ability will trigger only once.",
        )
        ruling(
            "2021-04-16",
            "If you gain an amount of life \"for each\" of something, that life is gained as one event and " +
                "the ability will trigger only once.",
        )
    }
}

val ValentinDeanOfTheVein: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = ValentinDeanOfTheVeinFront,
    backFace = LisetteDeanOfTheRootBack,
)
