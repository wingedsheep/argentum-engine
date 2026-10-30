package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ashiok, Nightmare Muse
 * {3}{U}{B}
 * Legendary Planeswalker — Ashiok
 * Loyalty 5
 * +1: Create a 2/3 blue and black Nightmare creature token with "Whenever this token attacks or
 * blocks, each opponent exiles the top two cards of their library."
 * −3: Return target nonland permanent to its owner's hand, then that player exiles a card from
 * their hand.
 * −7: You may cast up to three spells from among face-up cards your opponents own from exile
 * without paying their mana costs.
 *
 * Modeling notes:
 *  - +1: the token carries its own trigger, `Triggers.or(self.attacks(), self.blocks())` (the
 *    Imprison disjunction), whose effect is Malboro's `ForEachPlayer(EachOpponent, exileTop(2))` —
 *    the iteration rebinds the controller, so each opponent exiles from their own library.
 *  - −3: Recoil's bounce-then-"that player", with Karn Liberated's exile-from-hand pipeline. The
 *    owner of the bounced permanent is [Player.OwnerOf] the target; [Chooser.ControllerOfTarget]
 *    falls back to that owner once the card has left the battlefield, so the owner picks. The pool
 *    is `nontoken()`: a bounced token sits in hand until state-based actions remove it, and the
 *    ruling says the player can't exile it — they must exile a real card if able. The returned
 *    card itself may be the one exiled (ruling).
 *  - −7: Invoke Calamity's capped free-cast loop ([Effects.CastUpToNFromCollectionWithoutPayingCost]
 *    with `maxCasts = 3`) over every face-up nonland card your opponents own in exile — not only
 *    those Ashiok exiled (ruling). The casts happen during resolution, ignoring timing (rulings).
 */
val AshiokNightmareMuse = card("Ashiok, Nightmare Muse") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Ashiok"
    startingLoyalty = 5
    oracleText = "+1: Create a 2/3 blue and black Nightmare creature token with \"Whenever this token " +
        "attacks or blocks, each opponent exiles the top two cards of their library.\"\n" +
        "−3: Return target nonland permanent to its owner's hand, then that player exiles a card from " +
        "their hand.\n" +
        "−7: You may cast up to three spells from among face-up cards your opponents own from exile " +
        "without paying their mana costs."

    // +1: Create a 2/3 blue and black Nightmare creature token with "Whenever this token attacks or
    //     blocks, each opponent exiles the top two cards of their library."
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 3,
            colors = setOf(Color.BLUE, Color.BLACK),
            creatureTypes = setOf("Nightmare"),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.or(Triggers.self.attacks(), Triggers.self.blocks()),
                    effect = Effects.ForEachPlayer(
                        Player.EachOpponent,
                        Patterns.Library.exileTop(2, EffectTarget.Controller),
                    ),
                    descriptionOverride = "Whenever this token attacks or blocks, each opponent exiles " +
                        "the top two cards of their library.",
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/c/5/c528581f-f671-45ae-8d7a-ae26a4e62ceb.jpg?1783931456",
        )
        description = "Create a 2/3 blue and black Nightmare creature token with \"Whenever this token " +
            "attacks or blocks, each opponent exiles the top two cards of their library.\""
    }

    // −3: Return target nonland permanent to its owner's hand, then that player exiles a card from
    //     their hand.
    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.NonlandPermanent)
        val thatPlayer = Player.OwnerOf("target nonland permanent")
        effect = Effects.ReturnToHand(permanent) then Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, thatPlayer, GameObjectFilter.Any.nontoken()))
            val chosen = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.ControllerOfTarget,
                prompt = "Choose a card to exile from your hand",
                selectedLabel = "Exile",
            )
            exile(chosen, owner = thatPlayer)
        }
        description = "Return target nonland permanent to its owner's hand, then that player exiles a card " +
            "from their hand."
    }

    // −7: You may cast up to three spells from among face-up cards your opponents own from exile
    //     without paying their mana costs.
    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val pool = gather(
                CardSource.FromZone(Zone.EXILE, Player.EachOpponent, GameObjectFilter.Nonland.faceUp())
            )
            run(Effects.CastUpToNFromCollectionWithoutPayingCost(pool, maxCasts = 3))
        }
        description = "You may cast up to three spells from among face-up cards your opponents own from " +
            "exile without paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/112eb5ce-4fe5-45b3-b810-79fef74b07f9.jpg?1783931525"
        ruling("2020-01-24", "Any effect that exiles a card exiles it face up unless that effect specifies otherwise.")
        ruling("2020-01-24", "The card exiled while resolving Ashiok's second ability may be the one that was returned to its owner's hand.")
        ruling(
            "2020-01-24",
            "If Ashiok's second ability returns a token to its owner's hand, that player can't exile that token " +
                "from their hand. They must exile a card from their hand if possible."
        )
        ruling(
            "2020-01-24",
            "You may cast any face-up cards your opponents own from exile while resolving Ashiok's last ability, " +
                "not just those exiled by Ashiok's second ability or by Ashiok's Nightmare tokens."
        )
        ruling(
            "2020-01-24",
            "If you wish to cast any exiled cards, you do so as part of the resolution of Ashiok's last ability. " +
                "You can't wait to cast them later. Timing permissions based on a card's type are ignored."
        )
        ruling(
            "2020-01-24",
            "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory " +
                "additional costs, such as that of Thrill of Possibility, those must be paid to cast the card."
        )
        ruling("2020-01-24", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
