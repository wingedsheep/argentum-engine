package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Saheeli, Sublime Artificer — War of the Spark #234 (canonical printing)
 * {1}{U/R}{U/R} · Legendary Planeswalker — Saheeli · Loyalty 5
 *
 * Whenever you cast a noncreature spell, create a 1/1 colorless Servo artifact creature token.
 * −2: Target artifact you control becomes a copy of another target artifact or creature you
 * control until end of turn, except it's an artifact in addition to its other types.
 *
 * The −2 is True Polymorph's two-target copy shape (index 0 `affected` becomes a copy of index 1,
 * wrapped in [TargetOther] for "another") with Mindlink Mech's end-of-turn duration and
 * "artifact in addition to its other types" copy exception — a copiable value (CR 707.9b), so a
 * later copy of the artifact is an artifact too, per the ruling.
 */
val SaheeliSublimeArtificer = card("Saheeli, Sublime Artificer") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Saheeli"
    startingLoyalty = 5
    oracleText = "Whenever you cast a noncreature spell, create a 1/1 colorless Servo artifact creature token.\n" +
        "−2: Target artifact you control becomes a copy of another target artifact or creature you control " +
        "until end of turn, except it's an artifact in addition to its other types."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/6/761507d5-d36a-4123-a074-95d7f6ffb4c5.jpg?1783933348"
        )
    }

    loyaltyAbility(-2) {
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
        val copySource = target(TargetOther(
            baseRequirement = TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()))
        ))
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = copySource,
            duration = Duration.EndOfTurn,
            affected = artifact,
            exceptions = CopyExceptions(addedCardTypes = setOf(CardType.ARTIFACT)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a10b543-d5d4-42a8-9ee8-dada59a2ad7e.jpg?1783933375"
        ruling("2019-05-03", "Saheeli's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-05-03", "Saheeli's loyalty ability causes the target artifact to copy the printed values of the target permanent, plus any copy effects that have been applied to it. It won't copy counters on that permanent or effects that have changed its power, toughness, types, color, or so on. Notably, it won't copy effects that made the target permanent become a creature.")
        ruling("2019-05-03", "The target artifact is an artifact in addition to whatever types the second target has, and this exception is copiable. If something else copies the artifact later in the turn, that copy also will be an artifact.")
        ruling("2019-05-03", "If the target artifact copies a permanent that's copying something else, it will become whatever the target is copying.")
        ruling("2019-05-03", "If an effect begins to apply to the target artifact before it becomes a copy of another permanent, that effect will continue to apply.")
        ruling("2019-05-03", "If the target artifact becomes a creature the same turn it enters the battlefield, you can't attack with it or use any of its {T} abilities unless it has haste.")
        ruling("2019-05-03", "If the target artifact isn't an Equipment and becomes a copy of an Equipment, it'll become unattached when it becomes a non-Equipment artifact again.")
        ruling("2019-05-03", "If the target artifact is an Equipment attached to a creature and it becomes a copy of a non-Equipment permanent, it becomes unattached. If it becomes a copy of an Equipment, it remains attached.")
    }
}
