package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ExiledSpellRider
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Feather, the Redeemed
 * {R}{W}{W}
 * Legendary Creature — Angel
 * 3/4
 *
 * Flying
 * Whenever you cast an instant or sorcery spell that targets a creature you control, exile that
 * card instead of putting it into your graveyard as it resolves. If you do, return it to your hand
 * at the beginning of the next end step.
 *
 * The trigger marks the spell with [Effects.MarkSpellExileOnResolve]
 * ([ExiledSpellRider.RETURN_TO_HAND_AT_NEXT_END_STEP]): it resolves fully, is exiled instead of
 * going to the graveyard, and a delayed trigger returns it at the next end step — even if Feather
 * has left the battlefield by then. A spell that doesn't resolve, exiles itself, or that you don't
 * own is not exiled or returned (rulings).
 */
val FeatherTheRedeemed = card("Feather, the Redeemed") {
    manaCost = "{R}{W}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast an instant or sorcery spell that targets a creature you control, exile that " +
        "card instead of putting it into your graveyard as it resolves. If you do, return it to your " +
        "hand at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl()))
        )
        effect = Effects.MarkSpellExileOnResolve(ExiledSpellRider.RETURN_TO_HAND_AT_NEXT_END_STEP)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4a2d2c6-8eaa-4760-b620-921b807baa2e.jpg?1783933396"

        ruling("2021-03-19", "If Feather is still on the battlefield as you finish casting an instant or sorcery spell that targets one or more creatures you control, its ability triggers. The replacement effect that exiles that spell and the delayed triggered ability that returns it to your hand both take effect even if Feather leaves the battlefield after this point.")
        ruling("2021-03-19", "The spell may have any other targets in addition to a creature you control.")
        ruling("2021-03-19", "If an instant or sorcery spell you cast that targets your creature doesn't resolve for any reason (either because another spell or ability counters it or because all its targets are illegal as it tries to resolve), it won't be exiled. You won't return it to your hand.")
        ruling("2021-03-19", "If an instant or sorcery spell's own instructions tell you to exile it or put it anywhere else, it won't try to be put into your graveyard or exiled with Feather's effect, so you won't return it to your hand.")
        ruling("2021-03-19", "If another replacement effect instructs you to exile an instant or sorcery spell, such as that of Dreadhorde Arcanist or the flashback keyword, you may choose to apply Feather's replacement effect first. If you do, Feather's delayed triggered ability will return that card to your hand.")
        ruling("2021-03-19", "If you cast an instant or sorcery spell that you don't own, it won't try to be put into your graveyard, so you won't exile it with Feather's effect or return it to your hand.")
    }
}
