package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Emeritus of Ideation // Ancestral Recall — Secrets of Strixhaven #45
 * {3}{U}{U} · Creature — Human Wizard · 5/5
 *
 * Flying, ward {2}
 * This creature enters prepared.
 * Whenever this creature attacks, you may exile eight cards from your graveyard.
 *   If you do, this creature becomes prepared.
 * (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)
 * //
 * Ancestral Recall — {U}, Instant: Target player draws three cards.
 *
 * Prepare (Secrets of Strixhaven): enters with the PREPARED keyword. The attack ability's optional
 * exile-eight-from-graveyard payment re-prepares it via [Effects.BecomePrepared] only if eight cards
 * are actually exiled (modeled as MayEffect → IfYouDo gated on the pipeline moving eight cards).
 * Becoming prepared creates a copy of its prepare spell ("Ancestral Recall") in exile that its
 * controller may cast for {U}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val EmeritusOfIdeation = card("Emeritus of Ideation") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 5
    toughness = 5
    oracleText = "Flying, ward {2}\n" +
        "This creature enters prepared.\n" +
        "Whenever this creature attacks, you may exile eight cards from your graveyard. " +
        "If you do, this creature becomes prepared. " +
        "(While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.FLYING, Keyword.PREPARED)
    keywordAbility(KeywordAbility.ward("{2}"))

    // Whenever this creature attacks, you may exile eight cards from your graveyard.
    // If you do, this creature becomes prepared.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = Effects.Pipeline {
                    val grave = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
                    val chosen = chooseExactly(
                        count = 8,
                        from = grave,
                        prompt = "Exile eight cards from your graveyard",
                        name = "ideationExile"
                    )
                    exile(chosen)
                },
                ifYouDo = Effects.BecomePrepared(EffectTarget.Self),
                successCriterion = SuccessCriterion.CollectionNonEmpty("ideationExile", min = 8),
            ),
            descriptionOverride = "You may exile eight cards from your graveyard. " +
                "If you do, this creature becomes prepared.",
        )
    }

    // Ancestral Recall — the prepare spell. Target player draws three cards.
    prepare("Ancestral Recall") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Target player draws three cards."
        spell {
            val t = target("target", TargetPlayer())
            effect = Effects.DrawCards(3, t)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "45"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75961d36-acf6-425f-9698-0bf52af74f31.jpg?1778165058"
    }
}
