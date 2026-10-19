package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kotose, the Silent Spider
 * {3}{U}{B}
 * Legendary Creature — Human Ninja
 * 4/4
 *
 * When Kotose enters, exile target card other than a basic land card from an opponent's
 * graveyard. Search that player's graveyard, hand, and library for any number of cards with the
 * same name as that card and exile them. Then that player shuffles. For as long as you control
 * Kotose, you may play one of the exiled cards, and you may spend mana as though it were mana of
 * any color to cast it.
 *
 * The permission covers the target *and* the searched-out copies as one group, capped at a single
 * play (`maxPlays = 1`). The pipeline builds that group without a union step: the search runs
 * over the target's owner's graveyard while the target is still there, so `matches` holds the
 * target plus every same-named copy; the player picks among the copies other than the target, and
 * `matches` minus the unpicked remainder is exactly "the exiled cards".
 *
 * `MayPlayExpiry.WhileYouControlSource` is one-way (CR 611.2b): losing control of Kotose, or Kotose
 * leaving and returning, ends the permission for good — the ruling's exact behaviour.
 */
val KotoseTheSilentSpider = card("Kotose, the Silent Spider") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Ninja"
    oracleText = "When Kotose enters, exile target card other than a basic land card from an " +
        "opponent's graveyard. Search that player's graveyard, hand, and library for any number of " +
        "cards with the same name as that card and exile them. Then that player shuffles. For as " +
        "long as you control Kotose, you may play one of the exiled cards, and you may spend mana " +
        "as though it were mana of any color to cast it."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.CardInGraveyard.ownedByOpponent().nonbasic())
        val thatPlayer = Player.OwnerOf("target card")
        effect = Effects.Pipeline {
            val targetCard = gather(CardSource.ChosenTargets, name = "kotoseTarget")
            val cardName = storeCardName(targetCard, name = "kotoseName")
            // Every card with that name in the player's graveyard, hand, and library — the target
            // included, since it hasn't left the graveyard yet.
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = thatPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(cardName)
                ),
                name = "kotoseMatches",
                search = true
            )
            exile(targetCard, owner = thatPlayer)
            val copies = exclude(matches, targetCard, name = "kotoseCopies")
            val picked = chooseAnyNumberSplit(
                from = copies,
                prompt = "Choose any number of cards with that name to exile",
                selectedLabel = "Exile",
                showAllCards = true,
                alwaysPrompt = true,
                name = "kotoseSelected",
                remainderName = "kotoseLeft"
            )
            exile(picked.selected, owner = thatPlayer)
            run(Effects.ShuffleLibrary(target = EffectTarget.PlayerRef(thatPlayer)))
            // The exiled cards: the target plus the picked copies.
            val exiledCards = exclude(matches, picked.remainder, name = "kotoseExiled")
            run(
                Effects.GrantMayPlayFromExile(
                    from = exiledCards,
                    expiry = MayPlayExpiry.WhileYouControlSource("Kotose"),
                    maxPlays = 1,
                    withAnyManaColor = true
                )
            )
        }
        description = "When Kotose enters, exile target card other than a basic land card from an " +
            "opponent's graveyard. Search that player's graveyard, hand, and library for any number " +
            "of cards with the same name as that card and exile them. Then that player shuffles. For " +
            "as long as you control Kotose, you may play one of the exiled cards, and you may spend " +
            "mana as though it were mana of any color to cast it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/314e97b5-cd43-4f5e-acdd-a107208e061c.jpg?1783923832"
        ruling("2022-02-18", "You may play exactly one of the exiled cards. Once you play one of them, the others will remain in exile.")
        ruling("2022-02-18", "You may leave any cards with the same name as the target card in the zones they're in if you want.")
        ruling("2022-02-18", "If another player gains control of Kotose, the Silent Spider, you won't be able to play any of the exiled cards, even if you later regain control of it. Similarly, if Kotose leaves the battlefield and later returns, you won't be able to play the cards that were exiled the first time it was on the battlefield.")
    }
}
