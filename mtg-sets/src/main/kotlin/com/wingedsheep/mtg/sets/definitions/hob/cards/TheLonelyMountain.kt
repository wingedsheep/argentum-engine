package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Lonely Mountain — The Hobbit #187
 * Land — Mountain · Rare
 *
 * ({T}: Add {R}.)
 * This land enters tapped unless you control an Equipment.
 * {4}{R}, {T}: Create a 2/2 red Dwarf creature token. This ability costs {1} less to activate for
 * each Equipment you control. Activate only as a sorcery.
 *
 * Modeling notes:
 *  - The `{T}: Add {R}` is **not** authored: it is intrinsic to the Mountain land type (CR 305.6),
 *    the same way [com.wingedsheep.mtg.sets.definitions.gpt.cards.StompingGround] gets both of its
 *    mana abilities. Writing it out would double the ability.
 *  - The entry clause is a self-replacement with an `unlessCondition`, so the check happens as the
 *    land enters rather than as a triggered untap afterwards — a land put onto the battlefield by
 *    another effect is gated the same way.
 *  - The discount rides [com.wingedsheep.sdk.scripting.ActivatedAbility.genericCostReduction] (the
 *    Qiqirn Merchant shape): the count of Equipment you control is evaluated once at activation and
 *    reduces only the `{4}` — the `{R}` pip is never touched (CR 118.9a), so five Equipment leave the
 *    ability costing `{R}`, not free. The legal-action enumerator applies the same reduction, so what
 *    the client shows as affordable is what actually gets paid.
 *  - "Equipment" is `Artifact.withSubtype("Equipment")` in both places, which is the projected
 *    subtype — an artifact that has *become* an Equipment counts, and a token Equipment counts too.
 */
val TheLonelyMountain = card("The Lonely Mountain") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land — Mountain"
    oracleText = "({T}: Add {R}.)\n" +
        "This land enters tapped unless you control an Equipment.\n" +
        "{4}{R}, {T}: Create a 2/2 red Dwarf creature token. This ability costs {1} less to " +
        "activate for each Equipment you control. Activate only as a sorcery."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.YouControl(
                GameObjectFilter.Artifact.withSubtype("Equipment")
            )
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{R}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dwarf"),
            imageUri = "https://cards.scryfall.io/normal/front/9/f/9fcb3a3f-c0d4-43d4-8549-826a38bfa27d.jpg?1785497537",
        )
        genericCostReduction = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Artifact.withSubtype("Equipment")
        ).count()
        timing = TimingRule.SorcerySpeed
        description = "Create a 2/2 red Dwarf creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b39ebc4d-a01a-4401-ab3a-bf6142c93b47.jpg?1784543511"
    }
}
