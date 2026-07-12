package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Diregraf Scavenger
 * {3}{B}
 * Creature — Zombie Bear
 * 2/3
 *
 * Deathtouch
 * When this creature enters, exile up to one target card from a graveyard. If a creature card was
 * exiled this way, each opponent loses 2 life and you gain 2 life.
 *
 * ETB gather-then-move-then-conditional (the Soul-Shackled Zombie idiom): the single optional
 * target is gathered ([CardSource.ChosenTargets]) and exiled, then the drain is gated on the
 * exiled pile actually containing a creature ([ConditionalOnCollectionEffect] filtered to
 * [GameObjectFilter.Creature]) — so a whiffed or noncreature exile skips the life swing.
 */
val DiregrafScavenger = card("Diregraf Scavenger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Bear"
    power = 2
    toughness = 3
    oracleText = "Deathtouch\n" +
        "When this creature enters, exile up to one target card from a graveyard. If a creature " +
        "card was exiled this way, each opponent loses 2 life and you gain 2 life."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "up to one target card from a graveyard",
            TargetObject(
                count = 1,
                optional = true,
                filter = TargetFilter.CardInGraveyard
            )
        )
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.ChosenTargets,
                storeAs = "diregraf_exiled"
            ),
            MoveCollectionEffect(
                from = "diregraf_exiled",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            ConditionalOnCollectionEffect(
                collection = "diregraf_exiled",
                filter = GameObjectFilter.Creature,
                ifNotEmpty = Effects.Composite(
                    Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)),
                    Effects.GainLife(2)
                ),
                ifEmpty = Effects.Composite(emptyList())
            )
        )
        description = "When this creature enters, exile up to one target card from a graveyard. " +
            "If a creature card was exiled this way, each opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Manuel Castañón"
        flavorText = "It can barely feel the sword anymore."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f72eb127-3f4c-42ed-8ba6-b6d83ea18545.jpg?1782703116"
    }
}
