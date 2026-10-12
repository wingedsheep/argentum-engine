package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ezrim, Agency Chief — Murders at Karlov Manor #202
 * {1}{W}{W}{U}{U} · Legendary Creature — Archon Detective · 5/5 · Rare
 *
 * Flying
 * When Ezrim enters, investigate twice.
 * {1}, Sacrifice an artifact: Ezrim gains your choice of vigilance, lifelink, or hexproof until
 * end of turn.
 *
 * The two halves are deliberately self-feeding: the enters trigger banks two Clues, and Clues are
 * artifacts, so the activated ability's fodder arrives with the body. Nothing links them, though —
 * *any* artifact pays, and the Clues are equally free to be cracked for cards instead.
 *
 * "Gains your choice of vigilance, lifelink, or hexproof" is [ModalEffect.chooseOne] over three
 * no-target [Mode]s, the shape Butcher of the Horde established for the identical wording. Each
 * mode is a plain [Effects.GrantKeyword] on [EffectTarget.Self] whose default duration is already
 * end of turn, so the "until end of turn" clause needs nothing spelled out.
 *
 * Three points of rules pedantry the modelling has to respect:
 *
 * - **The choice is made on resolution**, not on activation — `ModalEffectExecutor` resolves an
 *   activated ability's modes when the ability resolves, which is what "your choice of" (as opposed
 *   to a printed "Choose one —") means. An opponent responding to the activation therefore can't
 *   see which keyword is coming.
 * - **Hexproof chosen in response to removal still saves it**, for the same reason: the grant lands
 *   before the removal spell resolves, and hexproof is checked on resolution.
 * - **Ezrim isn't an artifact**, so the sacrifice is a plain [Costs.Sacrifice] over
 *   [GameObjectFilter.Artifact] rather than the `SacrificeAnother` variant — there is no self to
 *   exclude, and no printed "another" to honour.
 */
val EzrimAgencyChief = card("Ezrim, Agency Chief") {
    manaCost = "{1}{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Archon Detective"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "When Ezrim enters, investigate twice. (To investigate, create a Clue token. It's an " +
        "artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "{1}, Sacrifice an artifact: Ezrim gains your choice of vigilance, lifelink, or hexproof " +
        "until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Investigate(2)
        description = "When Ezrim enters, investigate twice."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(GameObjectFilter.Artifact)
        )
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Ezrim gains vigilance until end of turn",
                effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self),
            ),
            EffectChoice(
                label = "Ezrim gains lifelink until end of turn",
                effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self),
            ),
            EffectChoice(
                label = "Ezrim gains hexproof until end of turn",
                effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self),
            )
        ))
        description = "{1}, Sacrifice an artifact: Ezrim gains your choice of vigilance, " +
            "lifelink, or hexproof until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9554d5f2-7a33-4734-8cf3-dfae2ccc3596.jpg?1783912850"

        ruling(
            "2024-02-02",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
        ruling(
            "2024-02-02",
            "Some abilities trigger \"whenever you sacrifice a Clue\". Those abilities trigger " +
                "whenever you sacrifice a Clue for any reason, not just to activate a Clue's " +
                "activated ability."
        )
    }
}
