package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valki, God of Lies // Tibalt, Cosmic Impostor
 *
 * Valki's enter trigger exiles one revealed creature card per opponent with
 * [Effects.MoveUntilSourceLeaves]: the return is a duration, not a leaves trigger, so it still
 * happens after Valki has become a copy of something else and lost its printed abilities. The
 * `{X}` ability chooses among Valki's linked-exile pile by mana value and makes Valki a copy of the
 * card indefinitely.
 *
 * Tibalt gets his emblem as he enters ([OnEnterRun]), and every loyalty ability exiles into his
 * linked-exile pile. The emblem's `exiledWithSource()` names the Tibalt that created it, so its
 * cards stay playable after that Tibalt leaves the battlefield.
 */
private val ValkiGodOfLiesFront = card("Valki, God of Lies") {
    manaCost = "{1}{B}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — God"
    power = 2
    toughness = 1
    oracleText = "When Valki enters, each opponent reveals their hand. For each opponent, exile a " +
        "creature card they revealed this way until Valki leaves the battlefield.\n" +
        "{X}: Choose a creature card exiled with Valki with mana value X. Valki becomes a copy of that card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.Pipeline {
                run(Effects.RevealHand(EffectTarget.Controller))
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val creature = chooseExactly(
                    1,
                    from = hand,
                    chooser = Chooser.SourceController,
                    filter = GameObjectFilter.Creature,
                    prompt = "Choose a creature card to exile until Valki leaves the battlefield",
                    selectedLabel = "Exile",
                    showAllCards = true,
                )
                run(Effects.MoveUntilSourceLeaves(creature.asTarget, Zone.EXILE))
            },
        )
        description = "When Valki enters, each opponent reveals their hand. For each opponent, exile a " +
            "creature card they revealed this way until Valki leaves the battlefield."
    }

    activatedAbility {
        cost = Costs.Mana("{X}")
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val chosen = chooseExactly(
                1,
                from = exiled,
                filter = GameObjectFilter.Creature.manaValueEqualsX(),
                prompt = "Choose a creature card exiled with Valki for Valki to copy",
                selectedLabel = "Copy",
            )
            run(
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = chosen.asTarget,
                    duration = Duration.Permanent,
                    affected = EffectTarget.Self,
                    sourceFromAnyZone = true,
                )
            )
        }
        description = "{X}: Choose a creature card exiled with Valki with mana value X. Valki becomes a copy of that card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7e4c65-b4c4-4795-9475-3cba71c50ea5.jpg?1783928244"
        ruling("2021-02-05", "If Valki leaves the battlefield before its enters-the-battlefield ability resolves, each opponent will reveal their hand, but no cards will be exiled.")
        ruling("2021-02-05", "If there are no creature cards exiled with Valki with mana value equal to the value of X as Valki's activated ability resolves, nothing happens. God of Lies, indeed.")
        ruling("2021-02-05", "Valki copies the printed values of the exiled creature card. Notably, once Valki becomes a copy of another creature card, he won't have his own printed activated ability.")
    }
}

private val TibaltCosmicImpostorBack = card("Tibalt, Cosmic Impostor") {
    manaCost = "{5}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Planeswalker — Tibalt"
    startingLoyalty = 5
    oracleText = "As Tibalt enters, you get an emblem with \"You may play cards exiled with Tibalt, " +
        "Cosmic Impostor, and you may spend mana as though it were mana of any color to cast those spells.\"\n" +
        "+2: Exile the top card of each player's library.\n" +
        "−3: Exile target artifact or creature.\n" +
        "−8: Exile all graveyards. Add {R}{R}{R}."

    replacementEffect(
        OnEnterRun(
            Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(
                    MayPlayCardsFromExile(GameObjectFilter.Any.exiledWithSource(), withAnyManaColor = true)
                ),
                emblemDescription = "You may play cards exiled with Tibalt, Cosmic Impostor, and you may " +
                    "spend mana as though it were mana of any color to cast those spells.",
            )
        )
    )

    loyaltyAbility(+2) {
        effect = Effects.Pipeline {
            val tops = gather(CardSource.TopOfLibrary(count = 1, player = Player.Each))
            exile(tops, linkToSource = true)
        }
        description = "Exile the top card of each player's library."
    }

    loyaltyAbility(-3) {
        val t = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.ExileLinkedToSource(t)
    }

    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val graveyards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each))
            exile(graveyards, linkToSource = true)
            run(Effects.AddMana(Color.RED, 3))
        }
        description = "Exile all graveyards. Add {R}{R}{R}."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/back/e/a/ea7e4c65-b4c4-4795-9475-3cba71c50ea5.jpg?1783928244"
        ruling("2021-02-05", "The emblem given to you by Tibalt allows you to play cards exiled with that specific Tibalt, Cosmic Impostor, even after that Tibalt leaves the battlefield. If a different Tibalt, Cosmic Impostor comes under your control, it's a new object (even if it's represented by the same card). Of course, the new Tibalt will also give you an emblem so you can play the cards he exiles.")
        ruling("2021-02-05", "The cards exiled by Tibalt's loyalty abilities are all exiled face up.")
        ruling("2021-02-05", "While resolving Tibalt's last ability, you'll add {R}{R}{R} even if you don't exile any cards.")
    }
}

val ValkiGodOfLies: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = ValkiGodOfLiesFront,
    backFace = TibaltCosmicImpostorBack,
)
