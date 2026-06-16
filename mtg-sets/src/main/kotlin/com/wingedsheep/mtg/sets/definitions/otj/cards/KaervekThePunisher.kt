package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kaervek, the Punisher
 * {1}{B}{B}
 * Legendary Creature — Human Warlock
 * 3/3
 *
 * Whenever you commit a crime, exile up to one target black card from your graveyard and copy
 * it. You may cast the copy. If you do, you lose 2 life. (Targeting opponents, anything they
 * control, and/or cards in their graveyards is a crime. Copies of permanent spells become tokens.)
 *
 * Built from the same "copy a card in a zone, then cast the copy" atomic chain as Shiko, Paragon
 * of the Way (Rule 707.12), with two card-specific twists:
 *
 *   1. **Exile + copy.** [Effects.Move] the targeted graveyard card to exile, then
 *      [Effects.CopyCardIntoCollection] makes a stack-style copy in exile (a copy of a permanent
 *      spell becomes a token when cast; an instant/sorcery copy ceases to exist).
 *   2. **You may cast the copy — paying its cost.** Unlike Shiko, Kaervek doesn't say "without
 *      paying its mana cost", so this uses [Effects.CastFromCollection] (payManaCost) inside a
 *      [MayEffect]. The cast routes through the normal machinery (X / targets / modes prompt).
 *   3. **If you do, you lose 2 life.** [Effects.CastFromCollection] publishes the cast card to the
 *      `kaervekCast` collection on a successful cast; the [IfYouDoEffect] gates the life loss on
 *      [SuccessCriterion.CollectionNonEmpty] so declining (or being unable to pay) loses no life.
 *
 * "Up to one target" → an optional target requirement (minCount 0); a copy that's declined or
 * never cast ceases to exist via the Rule 707.10a state-based action, leaving nothing in exile.
 */
val KaervekThePunisher = card("Kaervek, the Punisher") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Warlock"
    power = 3
    toughness = 3
    oracleText = "Whenever you commit a crime, exile up to one target black card from your " +
        "graveyard and copy it. You may cast the copy. If you do, you lose 2 life. (Targeting " +
        "opponents, anything they control, and/or cards in their graveyards is a crime. Copies " +
        "of permanent spells become tokens.)"

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        description = "Whenever you commit a crime, exile up to one target black card from your " +
            "graveyard and copy it. You may cast the copy. If you do, you lose 2 life."
        val exiledCard = target(
            "up to one target black card from your graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.Any.withColor(Color.BLACK).ownedByYou(),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.Composite(
            Effects.Move(exiledCard, Zone.EXILE),
            Effects.CopyCardIntoCollection(exiledCard, storeAs = "copy"),
            MayEffect(
                IfYouDoEffect(
                    action = Effects.CastFromCollection("copy", storeCastTo = "kaervekCast"),
                    ifYouDo = Effects.LoseLife(2, EffectTarget.Controller),
                    successCriterion = SuccessCriterion.CollectionNonEmpty("kaervekCast"),
                ),
                descriptionOverride = "You may cast the copy. If you do, you lose 2 life.",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f3affc1-be42-48c7-89ff-b59550ff278c.jpg?1712355608"

        ruling("2024-04-12", "You cast the copy while the ability is resolving and still on the stack. You can't wait to cast it later in the turn.")
        ruling("2024-04-12", "Because you're paying the spell's costs, if the spell has {X} in its mana cost, you may choose its value as normal.")
        ruling("2024-04-12", "If you don't want to cast the copy, you can choose not to; the copy ceases to exist the next time state-based actions are performed.")
        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
    }
}
